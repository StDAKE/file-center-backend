package com.huaixv06.fileCenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huaixv06.fileCenter.common.ErrorCode;
import com.huaixv06.fileCenter.constant.CommonConstant;
import com.huaixv06.fileCenter.exception.BusinessException;
import com.huaixv06.fileCenter.mapper.FileFavourMapper;
import com.huaixv06.fileCenter.mapper.FileMapper;
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
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
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


    @Override
    public void validFile(File file, boolean add) {
        if (file == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String name = file.getName();
        String fileType = file.getFileType();
        byte[] content = file.getContent();
        // 创建时，所有参数必须非空
        if(add && (StringUtils.isAnyBlank(name, fileType) || ObjectUtils.isEmpty(content))) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR);
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
    public FileVO getFileVO(File file, HttpServletRequest request) {
        FileVO fileVO = FileVO.objToVo(file);
        long fileId = file.getId();
        // 已登录，获取用户收藏状态
        User loginUser = userService.getLoginUser(request);
        if (loginUser != null) {
            // 获取收藏
            QueryWrapper<FileFavour> fileFavourQueryWrapper = new QueryWrapper<>();
            fileFavourQueryWrapper.in("fileId", fileId);
            fileFavourQueryWrapper.eq("userId", loginUser.getId());
            FileFavour fileFavour = fileFavourMapper.selectOne(fileFavourQueryWrapper);
            fileVO.setHasFavour(fileFavour != null);
        }
        return fileVO;
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

    @Override
    public Page<FileVO> listFileVOByPage(FileQueryRequest fileQueryRequest, HttpServletRequest request) {
        File fileQuery = new File();
        BeanUtils.copyProperties(fileQueryRequest, fileQuery);
        long current = fileQueryRequest.getCurrent();
        long pageSize = fileQueryRequest.getPageSize();
        String sortField = fileQueryRequest.getSortField();
        String sortOrder = fileQueryRequest.getSortOrder();
        String name = fileQuery.getName();
        String fileType = fileQuery.getFileType();
        QueryWrapper<File> queryWrapper = new QueryWrapper<>(fileQuery);
        if (StringUtils.isNotBlank(fileType)) {
            queryWrapper.like("fileType", fileType);
        }
        if (StringUtils.isNotBlank(name)){
            queryWrapper.like("name", name);
        }
        // 排除 'content' 字段
        queryWrapper.select(File.class, info -> !info.getColumn().equals("content"));
        queryWrapper.orderBy(StringUtils.isNotBlank(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);
        Page<File> filePage = this.page(new Page<>(current, pageSize), queryWrapper);

        return this.getFileVOPage(filePage, request);
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
//        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    @Override
    public void saveInEs(File file) {
//        String fileIlk = file.getFileIlk();
//        byte[] fileCnt = file.getContent();
//        FileEsDTO fileEsDTO = FileEsDTO.objToDto(file);
//        if(fileCnt == null){
//            fileEsDTO.setIsDelete(0);
//            fileEsDao.save(fileEsDTO);
//        }
//        // 如果是 docx 文件
//        if (fileIlk.equals("docx")) {
//            Map<String, String> contentDocx = getContentDocx(file);
//            if (contentDocx.get("result").equals("1")){
//                throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取正文失败");
//            }
//            String content = contentDocx.get("content");
//            fileEsDTO.setContent(content);
//            fileEsDTO.setIsDelete(0);
//            fileEsDao.save(fileEsDTO);
//        }
//        // 如果是 doc 文件
//        if (fileIlk.equals("doc")) {
//            Map<String, String> contentDoc = getContentDoc(file);
//            if (contentDoc.get("result").equals("1")){
//                throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取正文失败");
//            }
//            String content = contentDoc.get("content");
//            fileEsDTO.setContent(content);
//            fileEsDTO.setIsDelete(0);
//            fileEsDao.save(fileEsDTO);
//        }
//        // 如果是 wps 文件
//        if (fileIlk.equals("wps")) {
//            Map<String, String> contentWps = getContentWps(file);
//            if (contentWps.get("result").equals("1")) {
//                throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取正文失败");
//            }
//            String content = contentWps.get("content");
//            fileEsDTO.setContent(content);
//            fileEsDTO.setIsDelete(0);
//            fileEsDao.save(fileEsDTO);
//        }
//        if(fileIlk.equals("pdf")){
//            byte[] fileContent = file.getContent();
//            //目标文件
//            String filePath = "src/main/resources/static/" + file.getName() + ".pdf";
//            java.io.File esFile = FileUtil.writeBytes(fileContent, filePath);
//            try {
//                byte[] writtenContent = Files.readAllBytes(esFile.toPath());
//                if (!Arrays.equals(fileContent, writtenContent)) {
//                    throw new RuntimeException("文件写入失败");
//                }
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//            String path = esFile.getPath();
//            String content = OCRPdf(path);
//            fileEsDTO.setContent(content);
//            fileEsDTO.setIsDelete(0);
//            fileEsDao.save(fileEsDTO);
//        }
    }

//    public static Map<String, String> getContentDocx(File file) {
//        Map<String, String> map = new HashMap<>();
//        StringBuilder content = new StringBuilder();
//        String result = "0";  // 0表示获取正常，1表示获取异常
//        InputStream is = null;
//        try {
//            byte[] fileContent = file.getContent();
//            String filePath = "src/main/resources/static/" + file.getName() + ".docx";
//            java.io.File esFile = FileUtil.writeBytes(fileContent, filePath);
//
//            if (!esFile.exists() || !esFile.canRead()) {
//                log.error("文件不存在或不可读: " + esFile.getPath());
//                result = "1";
//                return map;
//            }
//
//            is = new BufferedInputStream(Files.newInputStream(esFile.toPath()));
//            XWPFDocument xwpf = new XWPFDocument(is);
//            List<XWPFParagraph> paragraphs = xwpf.getParagraphs();
//
//            if (paragraphs != null && !paragraphs.isEmpty()) {
//                for (XWPFParagraph paragraph : paragraphs) {
//                    if (!paragraph.getParagraphText().startsWith("    ")) {
//                        content.append(paragraph.getParagraphText().trim()).append("\r\n");
//                    } else {
//                        content.append(paragraph.getParagraphText());
//                    }
//                }
//            }
//        } catch (ClosedByInterruptException e) {
//            log.error("文件读取被中断: " + e);
//            result = "1";
//        } catch (Exception e) {
//            log.error("docx解析正文异常: " + e);
//            result = "1"; // 出现异常
//        } finally {
//            if (is != null) {
//                try {
//                    is.close();
//                } catch (IOException e) {
//                    log.error("关闭输入流时发生异常: " + e);
//                }
//            }
//            map.put("result", result);
//            map.put("content", String.valueOf(content));
//        }
//        return map;
//    }


//    /**
//     * 获取正文文件内容，doc方法
//     *
//     * @param file
//     * @return
//     */
//    public static Map<String, String> getContentDoc(File file) {
//        Map<String, String> map = new HashMap<>();
//        StringBuilder content = new StringBuilder();
//        String result = "0";  // 0表示获取正常，1表示获取异常
//        InputStream is = null;
//        try {
//            byte[] fileContent = file.getContent();
//            //目标文件
//            String filePath = "src/main/resources/static/" + file.getName() + ".doc";
//            java.io.File esFile = FileUtil.writeBytes(fileContent, filePath);
//            //根据需求入参也可以改为文件路径，对应的输入流部分改为new File(路径)即可
//            is = Files.newInputStream(esFile.toPath());
//            // 2003版本的word
//            WordExtractor extractor = new WordExtractor(is);  // 2003版本 仅doc格式文件可处理，docx文件不可处理
//            String[] paragraphText = extractor.getParagraphText();   // 获取段落，段落缩进无法获取，可以在前添加空格填充
//            if (paragraphText != null) {
//                for (String paragraph : paragraphText) {
//                    if (!paragraph.startsWith("    ")) {
//                        content.append(paragraph.trim()).append("\r\n");
//                    } else {
//                        content.append(paragraph);
//                    }
//                }
//            }
//        } catch (Exception e) {
//            log.error("doc解析正文异常:" + e);
//            result = "1"; // 出现异常
//        } finally {
//            if (is != null) {
//                try {
//                    is.close();
//                } catch (IOException e) {
//                    log.error("" + e);
//                }
//            }
//            map.put("result", result);
//            map.put("content", content.toString());
//        }
//        return map;
//    }

//    /**
//     * 获取正文文件内容，wps方法
//     *
//     * @param file
//     * @return
//     */
//    public static Map<String, String> getContentWps(File file) {
//        Map<String, String> map = new HashMap<>();
//        StringBuilder content = new StringBuilder();
//        String result = "0";  // 0表示获取正常，1表示获取异常
//        InputStream is = null;
//        try {
//            byte[] fileContent = file.getContent();
//            //目标文件
//            String filePath = "src/main/resources/static/" + file.getName() + ".wps";
//            java.io.File esFile = FileUtil.writeBytes(fileContent, filePath);
//            //根据需求入参也可以改为文件路径，对应的输入流部分改为new File(路径)即可
//            is = Files.newInputStream(esFile.toPath());
//            // wps版本word
//            HWPFDocument hwpf = new HWPFDocument(is);
//            WordExtractor wordExtractor = new WordExtractor(hwpf);
//            // 文档文本内容
//            String[] paragraphText1 = wordExtractor.getParagraphText();
//            if (paragraphText1 != null) {
//                for (String paragraph : paragraphText1) {
//                    if (!paragraph.startsWith("    ")) {
//                        content.append(paragraph.trim()).append("\r\n");
//                    } else {
//                        content.append(paragraph);
//                    }
//                }
//            }
//        } catch (Exception e) {
//            log.error("wps解析正文异常:" + e);
//            result = "1"; // 出现异常
//        } finally {
//            if (is != null) {
//                try {
//                    is.close();
//                } catch (IOException e) {
//                    log.error("" + e);
//                }
//            }
//            map.put("result", result);
//            map.put("content", content.toString());
//        }
//        return map;
//    }

//    public String OCRPdf(String pdfFilePath) {
//        java.io.File file = new java.io.File(pdfFilePath);
//        StringBuilder resultBuilder = new StringBuilder();
//        try {
//            PDDocument doc = PDDocument.load(file);
//            PDFRenderer renderer = new PDFRenderer(doc);
//            int pageCount = doc.getNumberOfPages();
//            for (int i = 0; i < pageCount; i++) {
//                BufferedImage image = renderer.renderImageWithDPI(i, 296);
//                String randomFileName = UUID.randomUUID().toString(); // 生成唯一随机数
//                String directoryPath = "src/main/resources/static/"; // 目录路径
//                java.io.File directory = new java.io.File(directoryPath);
//                if (!directory.exists()) {
//                    directory.mkdirs(); // 如果目录不存在，创建它
//                }
//                String imagePath = directoryPath + randomFileName + ".png"; // 文件路径
//                java.io.File outputFile = new java.io.File(imagePath);
//                ImageIO.write(image, "PNG", outputFile);
//
//                // 使用 OCR 引擎识别图片
//                InferenceEngine engine = InferenceEngine.getInstance(Model.ONNX_PPOCR_V3);
//                OcrResult ocrResult = engine.runOcr(imagePath);
//                String trim = ocrResult.getStrRes().trim();
//                // 将每一页的 trim 结果添加到 StringBuilder 中
//                resultBuilder.append(trim).append(System.lineSeparator());
//
//                // 删除临时生成的图片
//                boolean delete = outputFile.delete();
//                if (!delete) {
//                    log.error("Failed to delete temporary image file: " + imagePath);
//                }
//
//            }
//            doc.close();
//        } catch (IOException e) {
//            log.error("pdf转换解析异常", e);
//        }
//        // 返回结果字符串
//        return resultBuilder.toString();
//    }

}





