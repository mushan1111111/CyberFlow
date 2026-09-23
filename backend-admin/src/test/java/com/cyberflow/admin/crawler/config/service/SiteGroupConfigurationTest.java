package com.cyberflow.admin.crawler.config.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SiteGroupConfigurationTest {
    @Test
    void acceptsDatabaseDefinedGroupNamesWithoutAnAbcdAllowList() {
        assertEquals("业务一组", CrawlerConfigService.normalizeUserGroup(" 业务一组 "));
        assertEquals("TEAM_7", CrawlerConfigService.normalizeUserGroup("TEAM_7"));
        assertThrows(IllegalArgumentException.class,
                () -> CrawlerConfigService.normalizeUserGroup("bad group"));
    }

    @Test
    void accountMergeMapHasOneUnambiguousPrimaryForEveryAlias() {
        assertEquals(Map.of("B-姓名", List.of("B-账号1", "B-账号2")),
                CrawlerConfigService.normalizeUserMergeMap(
                        Map.of(" B-姓名 ", List.of(" B-账号1 ", "B-账号2"))));

        assertThrows(IllegalArgumentException.class,
                () -> CrawlerConfigService.normalizeUserMergeMap(Map.of(
                        "B-姓名1", List.of("B-账号"),
                        "B-姓名2", List.of("B-账号"))));
        assertThrows(IllegalArgumentException.class,
                () -> CrawlerConfigService.normalizeUserMergeMap(Map.of(
                        "B-姓名1", List.of("B-姓名2"),
                        "B-姓名2", List.of("B-账号"))));
    }

    @Test
    void departedEmployeeAccountsAreNormalizedStrictly() {
        assertEquals(List.of("A-姓名", "B-姓名"),
                CrawlerConfigService.normalizeDepartedEmployees(List.of(" A-姓名 ", "B-姓名")));
        assertThrows(IllegalArgumentException.class,
                () -> CrawlerConfigService.normalizeDepartedEmployees(List.of("A-姓名", " A-姓名 ")));
        assertThrows(IllegalArgumentException.class,
                () -> CrawlerConfigService.normalizeDepartedEmployees(List.of("")));
        assertThrows(IllegalArgumentException.class,
                () -> CrawlerConfigService.normalizeDepartedEmployees(Map.of()));
    }
}
