import java.awt.BorderLayout;
import java.net.URL;

import javax.swing.JApplet;

import org.jb2011.lnf.beautyeye.BeautyEyeLNFHelper;

public class SwingSet2Applet extends JApplet {
    
    /* (non-Javadoc)
     * @see java.applet.Applet#init()
     */
    public void init() {
    	try
		{
    		BeautyEyeLNFHelper.launchBeautyEyeLNF();
		}
		catch (Exception e)
		{
			System.err.println("BeautyEyeLNF"+e.getMessage());
		}
    	
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(new SwingSet2(this), BorderLayout.CENTER);
    }

    /**
     * Gets the uRL.
     *
     * @param filename the filename
     * @return the uRL
     */
    public URL getURL(String filename) {
        URL codeBase = this.getCodeBase();
        URL url = null;
	
        try {
            url = new URL(codeBase, filename);
	    System.out.println(url);
        } catch (java.net.MalformedURLException e) {
            System.out.println("Error: badly specified URL");
            return null;
        }

        return url;
    }


}
