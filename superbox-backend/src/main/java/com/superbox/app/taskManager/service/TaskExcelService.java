package com.superbox.app.taskManager.service;

import com.superbox.app.taskManager.entity.Task;
import com.superbox.common.BusinessException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TaskExcelService {

    private final TaskService taskService;

    private static final String[] HEADERS = {
        "任务名称", "描述", "重要等级", "计划开始日期", "计划结束日期",
        "执行人", "协同人", "状态", "完成时间", "备注说明"
    };
    private static final String[] SAMPLE = {
        "示例：完成需求文档", "编写产品需求规格说明书", "重要",
        "2026-06-15", "2026-06-30", "张三", "李四,王五",
        "进行中", "", "这是一个示例任务"
    };

    public void downloadTemplate(HttpServletResponse response) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("任务导入模板");
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle sampleStyle = createSampleStyle(wb);

            // Header row
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(HEADERS[i]);
                c.setCellStyle(headerStyle);
            }

            // Sample data row
            Row sample = sheet.createRow(1);
            for (int i = 0; i < SAMPLE.length; i++) {
                Cell c = sample.createCell(i);
                c.setCellValue(SAMPLE[i]);
                c.setCellStyle(sampleStyle);
            }

            // Auto-size columns
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.setColumnWidth(i, 16 * 256);
            }

            setResponseHeader(response, "任务导入模板.xlsx");
            wb.write(response.getOutputStream());
        } catch (Exception e) {
            throw new RuntimeException("生成模板失败", e);
        }
    }

    public void exportTasks(String status, String priority, String keyword, String executor, String tagId, HttpServletResponse response) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("任务列表");
            CellStyle headerStyle = createHeaderStyle(wb);

            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(HEADERS[i]);
                c.setCellStyle(headerStyle);
            }

            List<Task> tasks = taskService.list(status, priority, keyword, executor, tagId, 1, 50000).getRecords();
            if (tasks.size() >= 50000) {
                throw new BusinessException(400, "导出数据量过大（已达到单次导出上限 50000 条），请缩小筛选范围后重试");
            }
            DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            Map<String, String> statusMap = Map.of(
                "not_started", "未开始", "in_progress", "进行中",
                "completed", "已完成", "cancelled", "已作废"
            );
            Map<String, String> priorityMap = Map.of("normal", "一般", "important", "重要");

            for (int i = 0; i < tasks.size(); i++) {
                Task t = tasks.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(t.getTitle() != null ? t.getTitle() : "");
                row.createCell(1).setCellValue(t.getDescription() != null ? t.getDescription() : "");
                row.createCell(2).setCellValue(priorityMap.getOrDefault(t.getPriority(), "一般"));
                row.createCell(3).setCellValue(t.getPlanStartDate() != null ? t.getPlanStartDate().format(dateFmt) : "");
                row.createCell(4).setCellValue(t.getPlanEndDate() != null ? t.getPlanEndDate().format(dateFmt) : "");
                row.createCell(5).setCellValue(t.getExecutor() != null ? t.getExecutor() : "");
                row.createCell(6).setCellValue(t.getCollaborators() != null ? t.getCollaborators() : "");
                row.createCell(7).setCellValue(statusMap.getOrDefault(t.getStatus(), "未开始"));
                row.createCell(8).setCellValue(t.getCompletedAt() != null ? t.getCompletedAt().format(dateFmt) : "");
                row.createCell(9).setCellValue(t.getRemarks() != null ? t.getRemarks() : "");
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.setColumnWidth(i, 16 * 256);
            }

            setResponseHeader(response, "任务列表.xlsx");
            wb.write(response.getOutputStream());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("导出失败", e);
        }
    }

    public Map<String, Object> importTasks(MultipartFile file) {
        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            Map<String, String> statusReverse = Map.of(
                "未开始", "not_started", "进行中", "in_progress",
                "已完成", "completed", "已作废", "cancelled"
            );
            Map<String, String> priorityReverse = Map.of("一般", "normal", "重要", "important");

            int created = 0, errors = 0;
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isEmptyRow(row)) continue;
                try {
                    Task task = new Task();
                    task.setTitle(getCellStr(row, 0));
                    task.setDescription(getCellStr(row, 1));
                    task.setPriority(priorityReverse.getOrDefault(getCellStr(row, 2), "normal"));
                    String psd = getCellStr(row, 3);
                    if (!psd.isEmpty()) task.setPlanStartDate(parseFlexibleDate(psd));
                    String ped = getCellStr(row, 4);
                    if (!ped.isEmpty()) task.setPlanEndDate(parseFlexibleDate(ped));
                    task.setExecutor(getCellStr(row, 5));
                    task.setCollaborators(getCellStr(row, 6));
                    task.setStatus(statusReverse.getOrDefault(getCellStr(row, 7), "not_started"));
                    String ca = getCellStr(row, 8);
                    if (!ca.isEmpty()) task.setCompletedAt(LocalDate.parse(ca).atStartOfDay());
                    task.setRemarks(getCellStr(row, 9));

                    if (task.getTitle() == null || task.getTitle().isBlank()) {
                        errors++;
                        continue;
                    }
                    taskService.create(task, null);
                    created++;
                } catch (Exception e) {
                    errors++;
                }
            }
            Map<String, Object> result = new HashMap<>();
            result.put("created", created);
            result.put("errors", errors);
            return result;
        } catch (Exception e) {
            throw new BusinessException(400, "导入失败: " + e.getMessage());
        }
    }

    private static final DateTimeFormatter[] DATE_FORMATS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        DateTimeFormatter.ofPattern("MM-dd-yyyy"),
        DateTimeFormatter.ofPattern("yyyyMMdd"),
        DateTimeFormatter.ISO_LOCAL_DATE,
    };

    private LocalDate parseFlexibleDate(String s) {
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(s.trim(), fmt);
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new BusinessException(400, "无法解析日期格式: " + s);
    }

    private String getCellStr(Row row, int idx) {
        Cell cell = row.getCell(idx);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double v = cell.getNumericCellValue();
                yield v == (long) v ? String.valueOf((long) v) : String.valueOf(v);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private boolean isEmptyRow(Row row) {
        for (int i = 0; i < HEADERS.length; i++) {
            if (!getCellStr(row, i).isEmpty()) return false;
        }
        return true;
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createSampleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private void setResponseHeader(HttpServletResponse response, String filename) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
    }
}
