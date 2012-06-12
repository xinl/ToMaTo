package edu.upenn.cis.tomato.runtime;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import netscape.javascript.JSObject;

public class JSHelloWorld3 extends JApplet {

	private static final long serialVersionUID = 1L;
	
	JTextArea txt = new JTextArea();
    JScrollPane scroll = new JScrollPane();
    JButton btn = new JButton("Update Page");
    JSObject jso;

    public JSHelloWorld3() {
        txt.setText("Hello World 3");
        getContentPane().setLayout(new  BorderLayout());
        scroll.getViewport().setView(txt);

        scroll.setHorizontalScrollBarPolicy(scroll.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setVerticalScrollBarPolicy(scroll.VERTICAL_SCROLLBAR_AS_NEEDED);

        getContentPane().add(scroll,BorderLayout.CENTER);
        getContentPane().add(btn,BorderLayout.SOUTH);
        getContentPane().setBackground(Color.darkGray);

        btn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(jso != null )
                    try {
                        jso.call("updateWebPage", new String[] {txt.getText()});
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }
            }
        });
    }

    public void init()
    {
        jso = JSObject.getWindow(this);
    }

    public void setText(String s)
    {
        txt.setText(s);
    }
}
