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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.slf4j.Logger;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
        if(id != null){
            boolQueryBuilder.filter(QueryBuilders.termQuery("id", id));
        }
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
        // 如果是doc dox wps文件
    }

    /**
     * 获取正文文件内容，docx方法
     *
     * @param file
     * @return
     */
//    public static Map<String, String> getContentDocx(File file) {
//        Map<String, String> map = new HashMap();
//        StringBuffer content = new StringBuffer("");
//        String result = "0";  // 0表示获取正常，1表示获取异常
//        InputStream is = null;
//        Logger logger = null;
//        try {
//            //根据需求入参也可以改为文件路径，对应的输入流部分改为new File(路径)即可
//            is = new FileInputStream(file);
//            // 2007版本的word
//            XWPFDocument xwpf = new XWPFDocument(is);    // 2007版本，仅支持docx文件处理
//            List<XWPFParagraph> paragraphs = xwpf.getParagraphs();
//            if (paragraphs != null && paragraphs.size() > 0) {
//                for (XWPFParagraph paragraph : paragraphs) {
//                    if (!paragraph.getParagraphText().startsWith("    ")) {
//                        content.append(paragraph.getParagraphText().trim()).append("\r\n");
//                    } else {
//                        content.append(paragraph.getParagraphText());
//                    }
//                }
//            }
//        } catch (Exception e) {
//            logger.error("docx解析正文异常:" + e);
//            result = "1"; // 出现异常
//        } finally {
//            if (is != null) {
//                try {
//                    is.close();
//                } catch (IOException e) {
//                    logger.error("" + e);
//                }
//            }
//            map.put("result", result);
//            map.put("content", String.valueOf(content));
//        }
//        return map;
//    }



}




