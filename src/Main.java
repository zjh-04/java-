import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import view.LoginFrame;

/**
 * 物品视角管理系统 — 主入口 (GUI 模式)。
 *
 * 三种管理模式：
 *   1. 长期主义 — 记录高价值物品，计算日均持有成本
 *   2. 囤货模式 — 管理消耗品库存、均价、补货预警
 *   3. 纯记录模式 — 记录拥有的物品及状态
 */
public class Main {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.out.println("[提示] 使用默认界面外观");
        }

        SwingUtilities.invokeLater(() -> {
            LoginFrame frmLogin = new LoginFrame();
            frmLogin.setVisible(true);
        });
    }
}
