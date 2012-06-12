package edu.upenn.cis.tomato.runtime;
import java.awt.*;
import javax.swing.*;

public class JSHelloWorld2 extends JApplet {
	
	private static final long serialVersionUID = 1L;
	
	JTextArea txt = new JTextArea();
    JScrollPane scroll = new JScrollPane();

    public JSHelloWorld2() {
        txt.setText("Hello World");
        getContentPane().setLayout(new  BorderLayout());
        scroll.getViewport().setView(txt);

        scroll.setHorizontalScrollBarPolicy(scroll.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setVerticalScrollBarPolicy(scroll.VERTICAL_SCROLLBAR_AS_NEEDED);

        getContentPane().add(scroll,BorderLayout.CENTER);
        getContentPane().setBackground(Color.darkGray);
    }

    public void setText(String s)
    {
        txt.setText(s);
    }
}
