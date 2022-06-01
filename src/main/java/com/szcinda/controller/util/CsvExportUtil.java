package com.szcinda.controller.util;

import org.apache.commons.collections4.CollectionUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class CsvExportUtil {

    static DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    /**
     * CSV文件列分隔符
     */
    public static final String CSV_COLUMN_SEPARATOR = ",";

    /**
     * CSV文件行分隔符
     */
    private static final String CSV_ROW_SEPARATOR = System.lineSeparator();


    /**
     * @param dataList 集合数据
     * @param titles   表头部数据
     * @param keys     表内容的键值
     * @param os       输出流
     */
    public static void doExport(List<Map<String, String>> dataList, String titles, List<String> keys, OutputStream os) throws Exception {

        // 保证线程安全
        StringBuilder buf = new StringBuilder();

        String[] titleArr = titles.split(CSV_COLUMN_SEPARATOR);
        // 组装表头
        for (String title : titleArr) {
            buf.append(title).append(CSV_COLUMN_SEPARATOR);
        }
        buf.append(CSV_ROW_SEPARATOR);
        // 组装数据
        if (CollectionUtils.isNotEmpty(dataList)) {
            for (Map<String, String> data : dataList) {
                for (String key : keys) {
                    buf.append(data.get(key)).append(CSV_COLUMN_SEPARATOR);
                }
                buf.append(CSV_ROW_SEPARATOR);
            }
        }
        // 写出响应
        os.write(buf.toString().getBytes(StandardCharsets.UTF_8));
        os.flush();
    }

    /**
     * 设置Header
     *
     * @param fileName
     * @param response
     * @throws Exception
     */
    public static void responseSetProperties(String fileName, HttpServletResponse response) throws Exception {
        // 设置文件后缀
        String fn = fileName + df.format(LocalDateTime.now()) + ".csv";
        // 读取字符编码
        String utf = "UTF-8";

        // 设置响应
        response.setContentType("application/ms-txt.numberformat:@");
        response.setCharacterEncoding(utf);
        response.setHeader("Pragma", "public");
        response.setHeader("Cache-Control", "max-age=30");
        response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(fn, utf));
    }
}
