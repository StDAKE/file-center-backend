package com.huaixv06.fileCenter.utils;

import org.apache.poi.ss.usermodel.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExcelUtils {

    // 私有构造函数
    private ExcelUtils() {
        // 防止实例化
    }

    public static List<String> parseUsernamesFromExcel(MultipartFile file) throws IOException {
        List<String> usernames = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);  // 获取第一个Sheet

            for (Row row : sheet) {
                // 跳过标题行（如果第一行是标题）
                if (row.getRowNum() == 0) continue;

                // 假设用户名在第一列（索引0）
                Cell cell = row.getCell(1);
                if (cell != null) {
                    String username = cell.getStringCellValue().trim();
                    if (!username.isEmpty()) {
                        usernames.add(username);
                    }
                }
            }
        }
        return usernames;
    }
}
