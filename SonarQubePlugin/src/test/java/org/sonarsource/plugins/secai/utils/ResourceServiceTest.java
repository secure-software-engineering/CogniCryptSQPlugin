package org.sonarsource.plugins.secai.utils;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

class ResourceServiceTest {

    @Test
    void testSingletonInstance() throws IOException {
        ResourceService service1 = ResourceService.getInstance();
        ResourceService service2 = ResourceService.getInstance();
        assertSame(service1, service2, "ResourceService should be a singleton");
    }

    @Test
    void testGetCrySLRules() throws IOException {
        ResourceService service = ResourceService.getInstance();
        assertTrue(service.getCrySLRules().endsWith("crysl_rules"));
    }

    @Test
    void testGetReportDirectory() throws IOException {
        ResourceService service = ResourceService.getInstance();
        assertTrue(service.getReportDirectory().endsWith("report"));
    }

    @Test
    void testGetResourceDir() throws IOException {
        ResourceService service = ResourceService.getInstance();
        assertTrue(service.getResourceDir().endsWith("secai"));
    }

    @Test
    void testExtractResourceThrowsOnMissing() throws IOException {
        ResourceService service = ResourceService.getInstance();
        File target = new File(service.getResourceDir(), "failtest");
        Exception e = assertThrows(IOException.class,
                () -> service.extractResource("not/found/resource", "fail", target));
        assertTrue(e.getMessage().contains("Resource not found"));
    }

    @Test
    void testExtractDirectoryThrowsOnMissing() throws IOException {
        ResourceService service = ResourceService.getInstance();
        File tempDir = new File(service.getResourceDir(), "failtest");
        Exception e = assertThrows(IOException.class, () -> service.extractDirectory("not/found/dir", tempDir, 0));
        assertTrue(e.getMessage().contains("Resource directory not found"));
    }
}
