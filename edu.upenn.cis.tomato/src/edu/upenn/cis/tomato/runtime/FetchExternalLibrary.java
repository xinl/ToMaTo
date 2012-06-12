package edu.upenn.cis.tomato.runtime;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import netscape.javascript.JSObject;

import java.io.*;
import java.net.*;


public class FetchExternalLibrary extends JApplet{

	private static final long serialVersionUID = 1L;
	
	JSObject jso; JButton btn = new JButton("Fetch Library");
	
	public void init()
    {
        jso = JSObject.getWindow(this);
    }
	
	public void alert(String s)
	{
		if(jso != null )
        try 
		{
        	jso.call("alert", new String[]{s});
        }
        catch (Exception ex) 
        {
        	ex.printStackTrace();
        }
	}
	
	public FetchExternalLibrary()
	{
		getContentPane().setLayout(new  BorderLayout());
		getContentPane().add(btn, BorderLayout.CENTER);
		btn.addActionListener(new ActionListener() 
		{
			public void actionPerformed(ActionEvent e) 
			{
				fetchExternalJSLibrary("http://www.seas.upenn.edu/~jianchan/liveconnect/script.js");
			}
        });
	}
	
    public void fetchExternalJSLibrary(String targetURLString)
    {
    	
    	
    	final URL targetURL;
    	String targetFile = "";
    	String s = null;
    	
    	InputStream is = null;
    	BufferedReader dis = null;
        
        try 
        {
            //------------------------------------------------------------//
            // Step 2:  Create the URL.                                   //
            //------------------------------------------------------------//
            // Note: Put your real URL here, or better yet, read it as a  //
            // command-line arg, or read it from a file.                  //
            //------------------------------------------------------------//
        	
        	targetURL = new URL(targetURLString);
        	
            //----------------------------------------------//
            // Step 3:  Open an input stream from the url.  //
            //----------------------------------------------//
        	
        	is = targetURL.openStream();
        	
        	//-------------------------------------------------------------//
            // Step 4:                                                     //
            //-------------------------------------------------------------//
            // Convert the InputStream to BufferedReader                   //
            //-------------------------------------------------------------//

            dis = new BufferedReader(new InputStreamReader(is));

            //------------------------------------------------------------//
            // Step 5:                                                    //
            //------------------------------------------------------------//
            // Now just read each record of the input stream, and print   //
            // it out.  Note that it's assumed that this problem is run   //
            // from a command-line, not from an application or applet.    //
            //------------------------------------------------------------//

           
            while ((s = dis.readLine()) != null) 
            {
            	targetFile = targetFile + "\n" + s;
            }
            
            alert(targetFile);
            System.out.println(targetFile);

         } catch (MalformedURLException mue) {

        	alert("Ouch - a MalformedURLException happened.");
            System.out.println("Ouch - a MalformedURLException happened.");
            mue.printStackTrace();
            System.exit(1);

         } catch (IOException ioe) {

        	alert("Ouch - an IOException happened.");
            System.out.println("Ouch - an IOException happened.");
            ioe.printStackTrace();
            System.exit(1);

         } catch (Exception e)
         {
        	 alert("Ouch - Something wrong happened.");
        	 alert(e.toString());
        	 alert(e.getMessage());
         }
         finally {

            //---------------------------------//
            // Step 6:  Close the InputStream  //
            //---------------------------------//

            try 
            {
               is.close();
               dis.close();
            } 
            catch (IOException ioe) 
            {
               // just going to ignore this one
            }

         } // end of 'finally' clause
    }
}
