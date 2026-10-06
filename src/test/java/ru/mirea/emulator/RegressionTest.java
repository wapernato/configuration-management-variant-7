package ru.mirea.emulator;

import org.junit.jupiter.api.Test;

/** Подключает проверки предыдущих этапов к переносимой Maven/JUnit сборке. */
class RegressionTest {
    @Test
    void parserConfigurationStartupAndCli() throws Exception {
        RegressionChecks.main(new String[0]);
    }

    @Test
    void csvAndVirtualPaths() throws Exception {
        VfsChecks.main(new String[0]);
    }

    @Test
    void basicCommandsAndSwingComponent() throws Exception {
        BasicChecks.main(new String[0]);
    }

    @Test
    void suppliedStartupScenarios() throws Exception {
        ScenarioChecks.main(new String[0]);
    }
}
