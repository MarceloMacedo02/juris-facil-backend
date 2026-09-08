package com.jurisfacil.organizations.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.jurisfacil.organizations.model.enums.ModuleCode;

class ModuleCodeTest {

    @Test
    void exposesCanonicalModuleCatalog() {
        assertThat(ModuleCode.values()).containsExactly(
                ModuleCode.CORE, ModuleCode.PROCESS, ModuleCode.DEADLINES, ModuleCode.DOCUMENTS,
                ModuleCode.TASKS, ModuleCode.AI_JURIDICA, ModuleCode.TRIBUNALS, ModuleCode.REPORTS,
                ModuleCode.BILLING);
    }
}
