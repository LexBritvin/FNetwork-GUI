package Windows;

import FunctionalNetwork.Edge;

import javax.swing.*;
import java.awt.event.*;

public class ConfigureEdge extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField alphaField;
    private JTextField kField;
    private boolean ok;
    private Edge edge;

    public ConfigureEdge(Edge edge) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        this.edge = edge;
        alphaField.setText(edge.alpha.toString());
        kField.setText(edge.k.toString());

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
            edge.alpha = Double.parseDouble(alphaField.getText());
            edge.k = Double.parseDouble(kField.getText());
            ok = true;
            dispose();
        }
        catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Введено неправильное значение.");
        }
    }

    private void onCancel() {
        ok = false;
        dispose();
    }

    public boolean showDialog() {
        setLocationRelativeTo(null);
        pack();
        setVisible(true);
        return ok;
    }

}
