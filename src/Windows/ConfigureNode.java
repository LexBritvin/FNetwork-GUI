package Windows;

import javax.swing.*;
import java.awt.event.*;

public class ConfigureNode extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField weightField;
    private JTextField nameField;
    private Object[] ok;
    private String name;
    private Double weight;

    public ConfigureNode(String name, Double weight) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        this.name = name;
        this.weight = weight;
        nameField.setText(name);
        weightField.setText(weight.toString());

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

// call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

// call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void onOK() {
        try {
            name = nameField.getText();
            weight = Double.parseDouble(weightField.getText());
            ok = new Object[2];
            ok[0] = name;
            ok[1] = weight;
            dispose();
        }
        catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Введено неправильное значение.");
        }
    }

    private void onCancel() {
        ok = null;
        dispose();
    }

    public Object[] showDialog() {
        setLocationRelativeTo(null);
        pack();
        setVisible(true);
        return ok;
    }
}
