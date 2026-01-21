package org.sonarsource.plugins.secai.utils;

import org.junit.jupiter.api.*;
import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ReportServiceTest {

    @Test
    void testSingletonInstance() throws IOException {
        ReportService instance1 = ReportService.getInstance();
        ReportService instance2 = ReportService.getInstance();
        assertSame(instance1, instance2, "ReportService should be a singleton");
    }

    @Test
    void testGetReportDir() throws IOException {
        ReportService service = ReportService.getInstance();
        String expected = System.getProperty("user.home") + File.separator + "secai" + File.separator + "reports";
        assertEquals(expected, service.getReportDir());
        // directory should exist
        assertTrue(new File(expected).exists());
    }

    @Test
    void testGetReportDirWithProjectKey() throws IOException {
        ReportService service = ReportService.getInstance();
        String path = service.getReportDir("My@Project:Key!");
        assertTrue(path.endsWith("My_Project_Key_"));
        assertTrue(new File(path).exists());
    }

    @Test
    void testGetCogniCryptReport() throws IOException {
        ReportService service = ReportService.getInstance();
        String path = service.getCogniCryptReport("Key-123");
        assertTrue(path.endsWith("Key-123" + File.separator + "CryptoAnalysis-Report.json"));
    }
}
