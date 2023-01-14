package appeng.test;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.FileSystems;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import org.apache.commons.io.output.CloseShieldOutputStream;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.junit.platform.reporting.open.xml.OpenTestReportGeneratingListener;

// Most of these don't matter as this mod never gets published
@Mod(
        modid = "appeng-tests",
        name = "AE2 Dev Tests",
        version = "1.0",
        dependencies = "required-after:appliedenergistics2")
public class AppengTestMod {
    @EventHandler
    public void onServerStarted(FMLServerStartedEvent startedEv) {
        MinecraftServer.getServer().addChatMessage(new ChatComponentText("Running AE2 unit tests..."));
        runTests();
        MinecraftServer.getServer().addChatMessage(new ChatComponentText("Running AE2 unit tests finished"));
    }

    public void runTests() {
        // https://junit.org/junit5/docs/current/user-guide/#launcher-api
        System.setProperty("junit.platform.reporting.open.xml.enabled", "true");
        final String testsXmlOutDir = FileSystems.getDefault()
                .getPath("./junit-out/")
                .toAbsolutePath()
                .toString();
        final File testsXmlOutDirFile = new File(testsXmlOutDir);
        testsXmlOutDirFile.mkdirs();
        System.setProperty("junit.platform.reporting.output.dir", testsXmlOutDir);
        final LauncherDiscoveryRequest discovery = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectPackage("appeng.test"))
                .build();
        final SummaryGeneratingListener summaryGenerator = new SummaryGeneratingListener();
        final OpenTestReportGeneratingListener xmlGenerator = new OpenTestReportGeneratingListener();
        try (LauncherSession session = LauncherFactory.openSession()) {
            final Launcher launcher = session.getLauncher();
            final TestPlan plan = launcher.discover(discovery);
            launcher.registerTestExecutionListeners(summaryGenerator, xmlGenerator);
            launcher.execute(plan);
        }
        TestExecutionSummary summary = summaryGenerator.getSummary();
        try (PrintWriter stderrWriter = new PrintWriter(new CloseShieldOutputStream(System.err), true)) {
            summary.printFailuresTo(stderrWriter, 32);
            summary.printTo(stderrWriter);
            stderrWriter.flush();
        }
        // Throw an exception if running via `runServer`
        if (summary.getTotalFailureCount() > 0
                && FMLCommonHandler.instance().getSide().isServer()) {
            throw new RuntimeException("Some of the unit tests failed to execute, check the log for details");
        }
    }
}