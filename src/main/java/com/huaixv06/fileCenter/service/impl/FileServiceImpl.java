package com.huaixv06.fileCenter.service.impl;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huaixv06.fileCenter.common.ErrorCode;
import com.huaixv06.fileCenter.constant.CommonConstant;
import com.huaixv06.fileCenter.esdao.FileEsDao;
import com.huaixv06.fileCenter.exception.BusinessException;
import com.huaixv06.fileCenter.mapper.FileFavourMapper;
import com.huaixv06.fileCenter.mapper.FileMapper;
import com.huaixv06.fileCenter.model.custom.CustomXWPFDocument;
import com.huaixv06.fileCenter.model.dto.file.FileEsDTO;
import com.huaixv06.fileCenter.model.dto.file.FileQueryRequest;
import com.huaixv06.fileCenter.model.dto.file.FileQueryRequestByEs;
import com.huaixv06.fileCenter.model.entity.File;
import com.huaixv06.fileCenter.model.entity.FileFavour;
import com.huaixv06.fileCenter.model.entity.User;
import com.huaixv06.fileCenter.model.vo.FileVO;
import com.huaixv06.fileCenter.service.FileService;
import com.huaixv06.fileCenter.service.UserService;
import com.huaixv06.fileCenter.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

/**
* @author 11517
* @description 针对表【file(文件)】的数据库操作Service实现
* @createDate 2024-10-13 16:36:31
*/
@Service
@Slf4j
public class FileServiceImpl extends ServiceImpl<FileMapper, File>
    implements FileService{

    @Resource
    private UserService userService;

    @Resource
    private FileFavourMapper fileFavourMapper;

    @Resource
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Resource
    private FileEsDao fileEsDao;

    @Override
    public void validFile(File file, boolean add) {
        if (file == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String name = file.getName();
        String fileType = file.getFileType();
        byte[] content = file.getContent();
        // 创建时，所有参数必须非空
        if(add) {
            if (StringUtils.isAnyBlank(name, fileType) || ObjectUtils.isEmpty(content)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR);
            }
        }
        if (StringUtils.isNotBlank(name) && name.length() > 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名过长");
        }
        if (StringUtils.isNotBlank(fileType) && fileType.length() > 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件分类过长");
        }
        if (ObjectUtils.isEmpty(content)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件内容不能为空");
        }
    }

    /**
     * 从 ES 查询文件
     *
     * @param fileQueryRequestByEs
     * @return
     */
    @Override
    public Page<File> searchFromEs(FileQueryRequestByEs fileQueryRequestByEs){
        // 获取参数
        Long id = fileQueryRequestByEs.getId();
        String name = fileQueryRequestByEs.getName();
        String content = fileQueryRequestByEs.getContent();
        String searchText = fileQueryRequestByEs.getSearchText();
        long current = fileQueryRequestByEs.getCurrent() - 1;
        long pageSize = fileQueryRequestByEs.getPageSize();
        String sortField = fileQueryRequestByEs.getSortField();
        String sortOrder = fileQueryRequestByEs.getSortOrder();
        // 构建查询条件
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        // 过滤
        boolQueryBuilder.filter(QueryBuilders.termQuery("isDelete", 0));
        // 按关键词检索
        if (StringUtils.isNotBlank(searchText)) {
            boolQueryBuilder.should(QueryBuilders.matchQuery("name", searchText));
            boolQueryBuilder.should(QueryBuilders.matchQuery("content", searchText));
            boolQueryBuilder.minimumShouldMatch(1);
        }
        if(StringUtils.isNotBlank(name)){
            boolQueryBuilder.filter(QueryBuilders.matchQuery("name", name));
            boolQueryBuilder.minimumShouldMatch(1);
        }
        if(StringUtils.isNotBlank(content)){
            boolQueryBuilder.must(QueryBuilders.matchQuery("content", content));
            boolQueryBuilder.minimumShouldMatch(1);
        }
        // 排序
        SortBuilder<?> sortBuilder = null;
        if(StringUtils.isNotBlank(sortField)){
            sortBuilder = SortBuilders.fieldSort(sortField);
            sortBuilder.order(CommonConstant.SORT_ORDER_ASC.equals(sortOrder) ? SortOrder.ASC : SortOrder.DESC);
        }
        // 分页
        org.springframework.data.domain.PageRequest pageRequest = PageRequest.of((int) current, (int) pageSize);
        // 构造查询
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(boolQueryBuilder)
                .withPageable(pageRequest).withSorts(sortBuilder).build();
        SearchHits<FileEsDTO> searchHits = elasticsearchRestTemplate.search(searchQuery, FileEsDTO.class);
        Page<File> page = new Page<>();
        page.setTotal(searchHits.getTotalHits());
        List<File> resourceList = new ArrayList<>();
        // 查出结果后，从 db 获取最新动态数据
        if (searchHits.hasSearchHits()) {
            List<SearchHit<FileEsDTO>> searchHitList = searchHits.getSearchHits();
            List<Long> fileIdList = searchHitList.stream().map(searchHit -> searchHit.getContent().getId())
                    .collect(Collectors.toList());
            // 从数据库中取出更完整的数据
            List<File> fileList = baseMapper.selectBatchIds(fileIdList);
            if (fileList != null) {
                Map<Long, List<File>> idFileMap = fileList.stream().collect(Collectors.groupingBy(File::getId));
                fileIdList.forEach(fileId -> {
                    if (idFileMap.containsKey(fileId)) {
                        resourceList.add(idFileMap.get(fileId).get(0));
                    } else {
                        // 从 es 清空 db 已物理删除的数据
                        String delete = elasticsearchRestTemplate.delete(String.valueOf(fileId), FileEsDTO.class);
                        log.info("delete file {}", delete);
                    }
                });
            }
        }
        page.setRecords(resourceList);
        return page;
    }

    @Override
    public Page<FileVO> getFileVOPage(Page<File> filePage, HttpServletRequest request) {
        List<File> fileList = filePage.getRecords();
        Page<FileVO> fileVOPage = new Page<>(filePage.getCurrent(), filePage.getSize(), filePage.getTotal());
        if (CollectionUtils.isEmpty(fileList)) {
            return fileVOPage;
        }
        // 1. 已登录，获取用户收藏状态
        Map<Long, Boolean> fileIdHasFavourMap = new HashMap<>();
        User loginUser = userService.getLoginUser(request);
        if (loginUser != null) {
            Set<Long> fileIdSet = fileList.stream().map(File::getId).collect(Collectors.toSet());
            loginUser = userService.getLoginUser(request);
            // 获取收藏
            QueryWrapper<FileFavour> fileFavourQueryWrapper = new QueryWrapper<>();
            fileFavourQueryWrapper.in("fileId", fileIdSet);
            fileFavourQueryWrapper.eq("userId", loginUser.getId());
            List<FileFavour> fileFavourList = fileFavourMapper.selectList(fileFavourQueryWrapper);
            fileFavourList.forEach(fileFavour -> fileIdHasFavourMap.put(fileFavour.getFileId(), true));
        }
        // 填充信息
        List<FileVO> fileVOList = fileList.stream().map(file -> {
            FileVO fileVO = FileVO.objToVo(file);
            fileVO.setHasFavour(fileIdHasFavourMap.getOrDefault(file.getId(), false));
            return fileVO;
        }).collect(Collectors.toList());
        fileVOPage.setRecords(fileVOList);
        return fileVOPage;
    }

    /**
     * 获取查询包装类
     *
     * @param fileQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<File> getQueryWrapper(FileQueryRequest fileQueryRequest) {
        QueryWrapper<File> queryWrapper = new QueryWrapper<>();
        if (fileQueryRequest == null) {
            return queryWrapper;
        }
        String searchText = fileQueryRequest.getSearchText();
        String sortField = fileQueryRequest.getSortField();
        String sortOrder = fileQueryRequest.getSortOrder();
        Long id = fileQueryRequest.getId();
        String name = fileQueryRequest.getName();
        String content = fileQueryRequest.getContent();
        Long userId = fileQueryRequest.getUserId();
        // 拼接查询条件
        if (StringUtils.isNotBlank(searchText)) {
            queryWrapper.like("name", searchText).or().like("content", searchText);
        }
        queryWrapper.like(StringUtils.isNotBlank(name), "name", name);
        queryWrapper.like(StringUtils.isNotBlank(content), "content", content);
        if (id!= 0) {
            queryWrapper.eq("id", id);
        }
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    @Override
    public void saveInEs(File file) {
        String fileIlk = file.getFileIlk();
        FileEsDTO fileEsDTO = FileEsDTO.objToDto(file);
        // 如果是 docx 文件
        if (fileIlk.equals("docx")) {
            Map<String, String> contentDocx = getContentDocx(file);
            if (contentDocx.get("result").equals("1")){
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取正文失败");
            }
            String content = contentDocx.get("content");
            fileEsDTO.setContent(content);
            fileEsDTO.setIsDelete(0);
            fileEsDao.save(fileEsDTO);
        }
        // 如果是 doc 文件
        if (fileIlk.equals("doc")) {
            Map<String, String> contentDoc = getContentDoc(file);
            if (contentDoc.get("result").equals("1")){
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取正文失败");
            }
            String content = contentDoc.get("content");
            fileEsDTO.setContent(content);
            fileEsDTO.setIsDelete(0);
            fileEsDao.save(fileEsDTO);
        }
        // 如果是 wps 文件
        if (fileIlk.equals("wps")) {
            Map<String, String> contentWps = getContentWps(file);
            if (contentWps.get("result").equals("1")) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取正文失败");
            }
            String content = contentWps.get("content");
            fileEsDTO.setContent(content);
            fileEsDTO.setIsDelete(0);
            fileEsDao.save(fileEsDTO);
        }
        // 如果是 pdf 文件
        if (fileIlk.equals("pdf")) {
            // 先转换为 word 文件
            byte[] fileContent = file.getContent();
            //目标文件
            String filePath = "src/main/resources/static/" + file.getName();
            java.io.File esFile = FileUtil.writeBytes(fileContent, filePath);
            String path = esFile.getPath();
            try {
                convertPdfToWord(path);
            } catch (InvalidFormatException e) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "pdf转word失败");
            }
            Map<String, String> contentDocx = getContentDocx(file);
            if (contentDocx.get("result").equals("1")){
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取正文失败");
            }
            String content = contentDocx.get("content");
            fileEsDTO.setContent(content);
            fileEsDTO.setIsDelete(0);
            fileEsDao.save(fileEsDTO);
        }
    }

    /**
     * 获取正文文件内容，docx方法
     *
     * @param file
     * @return
     */
    public static Map<String, String> getContentDocx(File file) {
        Map<String, String> map = new HashMap<>();
        StringBuilder content = new StringBuilder();
        String result = "0";  // 0表示获取正常，1表示获取异常
        InputStream is = null;
        try {
            byte[] fileContent = file.getContent();
            //目标文件
            String filePath = "src/main/resources/static/" + file.getName();
            java.io.File esFile = FileUtil.writeBytes(fileContent, filePath);
            //根据需求入参也可以改为文件路径，对应的输入流部分改为new File(路径)即可
            is = Files.newInputStream(esFile.toPath());
            // 2007版本的word
            XWPFDocument xwpf = new XWPFDocument(is);    // 2007版本，仅支持docx文件处理
            List<XWPFParagraph> paragraphs = xwpf.getParagraphs();
            if (paragraphs != null && !paragraphs.isEmpty()) {
                for (XWPFParagraph paragraph : paragraphs) {
                    if (!paragraph.getParagraphText().startsWith("    ")) {
                        content.append(paragraph.getParagraphText().trim()).append("\r\n");
                    } else {
                        content.append(paragraph.getParagraphText());
                    }
                }
            }
        } catch (Exception e) {
            log.error("docx解析正文异常:" + e);
            result = "1"; // 出现异常
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    log.error("" + e);
                }
            }
            map.put("result", result);
            map.put("content", String.valueOf(content));
        }
        return map;
    }

    /**
     * 获取正文文件内容，doc方法
     *
     * @param file
     * @return
     */
    public static Map<String, String> getContentDoc(File file) {
        Map<String, String> map = new HashMap<>();
        StringBuilder content = new StringBuilder();
        String result = "0";  // 0表示获取正常，1表示获取异常
        InputStream is = null;
        try {
            byte[] fileContent = file.getContent();
            //目标文件
            String filePath = "src/main/resources/static/" + file.getName();
            java.io.File esFile = FileUtil.writeBytes(fileContent, filePath);
            //根据需求入参也可以改为文件路径，对应的输入流部分改为new File(路径)即可
            is = Files.newInputStream(esFile.toPath());
            // 2003版本的word
            WordExtractor extractor = new WordExtractor(is);  // 2003版本 仅doc格式文件可处理，docx文件不可处理
            String[] paragraphText = extractor.getParagraphText();   // 获取段落，段落缩进无法获取，可以在前添加空格填充
            if (paragraphText != null) {
                for (String paragraph : paragraphText) {
                    if (!paragraph.startsWith("    ")) {
                        content.append(paragraph.trim()).append("\r\n");
                    } else {
                        content.append(paragraph);
                    }
                }
            }
        } catch (Exception e) {
            log.error("doc解析正文异常:" + e);
            result = "1"; // 出现异常
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    log.error("" + e);
                }
            }
            map.put("result", result);
            map.put("content", content.toString());
        }
        return map;
    }

    /**
     * 获取正文文件内容，wps方法
     *
     * @param file
     * @return
     */
    public static Map<String, String> getContentWps(File file) {
        Map<String, String> map = new HashMap<>();
        StringBuilder content = new StringBuilder();
        String result = "0";  // 0表示获取正常，1表示获取异常
        InputStream is = null;
        try {
            byte[] fileContent = file.getContent();
            //目标文件
            String filePath = "src/main/resources/static/" + file.getName();
            java.io.File esFile = FileUtil.writeBytes(fileContent, filePath);
            //根据需求入参也可以改为文件路径，对应的输入流部分改为new File(路径)即可
            is = Files.newInputStream(esFile.toPath());
            // wps版本word
            HWPFDocument hwpf = new HWPFDocument(is);
            WordExtractor wordExtractor = new WordExtractor(hwpf);
            // 文档文本内容
            String[] paragraphText1 = wordExtractor.getParagraphText();
            if (paragraphText1 != null) {
                for (String paragraph : paragraphText1) {
                    if (!paragraph.startsWith("    ")) {
                        content.append(paragraph.trim()).append("\r\n");
                    } else {
                        content.append(paragraph);
                    }
                }
            }
        } catch (Exception e) {
            log.error("wps解析正文异常:" + e);
            result = "1"; // 出现异常
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    log.error("" + e);
                }
            }
            map.put("result", result);
            map.put("content", content.toString());
        }
        return map;
    }

    /*
     * pdf转word
     */
    public static void convertPdfToWord(String pdfFilePath) throws InvalidFormatException {
        try (PDDocument pdf = PDDocument.load(new java.io.File(pdfFilePath))) {
            int pageNumber = pdf.getNumberOfPages();

            String docFileName = pdfFilePath.substring(0, pdfFilePath.lastIndexOf(".")) + ".docx";

            java.io.File file = new java.io.File(docFileName);
            if (!file.exists()) {
                file.createNewFile();
            }
            CustomXWPFDocument document = new CustomXWPFDocument();
            FileOutputStream fos = new FileOutputStream(docFileName);

            //提取每一页的图片和文字，添加到 word 中
            for (int i = 0; i < pageNumber; i++) {

                PDPage page = pdf.getPage(i);
                PDResources resources = page.getResources();

                Iterable<COSName> names = resources.getXObjectNames();
                for (COSName cosName : names) {
                    if (resources.isImageXObject(cosName)) {
                        PDImageXObject imageXObject = (PDImageXObject) resources.getXObject(cosName);
                        java.io.File outImgFile = new java.io.File("H:\\img\\" + System.currentTimeMillis() + ".jpg");
                        Thumbnails.of(imageXObject.getImage()).scale(0.9).rotate(0).toFile(outImgFile);


                        BufferedImage bufferedImage = ImageIO.read(outImgFile);
                        int width = bufferedImage.getWidth();
                        int height = bufferedImage.getHeight();
                        if (width > 600) {
                            double ratio = Math.round(width / 550.0);
                            System.out.println("缩放比ratio：" + ratio);
                            width = (int) (width / ratio);
                            height = (int) (height / ratio);

                        }

                        System.out.println("width: " + width + ",  height: " + height);
                        FileInputStream in = new FileInputStream(outImgFile);
                        byte[] ba = new byte[in.available()];
                        in.read(ba);
                        ByteArrayInputStream byteInputStream = new ByteArrayInputStream(ba);

                        XWPFParagraph picture = document.createParagraph();
                        //添加图片
                        document.addPictureData(byteInputStream, CustomXWPFDocument.PICTURE_TYPE_JPEG);
                        //图片大小、位置
                        document.createPicture(document.getAllPictures().size() - 1, width, height, picture);

                    }
                }

                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                //当前页中的文字
                String text = stripper.getText(pdf);


                XWPFParagraph textParagraph = document.createParagraph();
                XWPFRun textRun = textParagraph.createRun();
                textRun.setText(text);
                textRun.setFontFamily("仿宋");
                textRun.setFontSize(11);
                //换行
                textParagraph.setWordWrap(true);
            }
            document.write(fos);
            fos.close();
            pdf.close();
            System.out.println("pdf转换解析结束！！----");
        } catch (IOException e) {
            log.error("pdf转换解析异常",e);
        }
    }

}





