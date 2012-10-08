/*
 * Q2DataDialog.java
 * Copyright (C)  2003
 */
package jake2.qcommon;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Q2DataTool {
    static final String home = System.getProperty("user.home");
    static final String sep = System.getProperty("file.separator");
    static final String dataDir = home + sep + "Jake2";
    static final String baseq2Dir = dataDir + sep + "baseq2";

    private Vector<String> mirrorNames = new Vector<String>();
    private Vector<String> mirrorLinks = new Vector<String>();
    private byte[] buf = new byte[8192];

    public void testQ2Data() {
        initMirrors();
        for(int i=0; !isAvail() && i<mirrorNames.size(); i++) {
            try {
                install(i);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    void destroy() {

    }
    
    void setStatus(String text) {
        System.err.println(text);
        System.err.println();
    }

    boolean isAvail() {
        Cvar.Set("cddir", baseq2Dir);
        FS.setCDDir();
        return null != FS.LoadFile("pics/colormap.pcx");        
    }

    void initMirrors() {
        InputStream in = getClass().getResourceAsStream("/mirrors");
        BufferedReader r = new BufferedReader(new InputStreamReader(in));
        try {
            while (true) {
                String name = r.readLine();
                String value = r.readLine();
                if (name == null || value == null) break;
                mirrorNames.add(name);
                mirrorLinks.add(value);
            }
        } catch (Exception e) {} 
        finally {
            try {
                r.close();
            } catch (Exception e1) {}
            try {
                in.close();
            } catch (Exception e1) {}
        }
    }

    void install(int mirrorIdx) {
        final String mirrorName = mirrorNames.get(mirrorIdx);
        final String mirror = mirrorLinks.get(mirrorIdx);
        InputStream in = null;
        OutputStream out = null;
        File outFile = null;
        
        setStatus("downloading from "+mirrorName+": <"+mirror+">");

        File dir = null;
        try {
            dir = new File(dataDir);
            dir.mkdirs();
        }
        catch (Exception e) {}
        try {
            if (!dir.isDirectory() || !dir.canWrite()) {
                setStatus("can't write to " + dataDir);
                return;
            } 
        }
        catch (Exception e) {
            setStatus(e.getMessage());
            return;
        }

        try {
            URL url = new URL(mirror);
            URLConnection conn = url.openConnection();
            // int length = conn.getContentLength();

            in = conn.getInputStream();

            outFile = File.createTempFile("Jake2Data", ".zip");
            outFile.deleteOnExit();
            out = new FileOutputStream(outFile);

            copyStream(in, out);
        } catch (Exception e) {
            setStatus(e.getMessage());
            return;
        } finally {
            try {
                in.close();
            } catch (Exception e) {}
            try {
                out.close();
            } catch (Exception e) {}                
        }

        try {
            installData(outFile.getCanonicalPath());
        } catch (Exception e) {
            setStatus(e.getMessage());
            return;
        }


        try {
            if (outFile != null) outFile.delete();
        } catch (Exception e) {}

        setStatus("installation successful from "+mirrorName+": <"+mirror+">");
    }


    void installData(String filename) throws Exception {
        InputStream in = null;
        OutputStream out = null;
        try {
            ZipFile f = new ZipFile(filename);
            Enumeration<? extends ZipEntry> e = f.entries();
            while (e.hasMoreElements()) {
                ZipEntry entry = (ZipEntry)e.nextElement();
                String name = entry.getName();
                int i;
                if ((i = name.indexOf("/baseq2")) > -1 && name.indexOf(".dll") == -1) {
                    name = dataDir + name.substring(i);
                    File outFile = new File(name);
                    if (entry.isDirectory()) {
                        outFile.mkdirs();
                    } else {
                        setStatus("installing " + outFile.getName());
                        outFile.getParentFile().mkdirs();
                        out = new FileOutputStream(outFile);
                        in = f.getInputStream(entry);
                        copyStream(in, out);
                    }
                }
            }
        } catch (Exception e) {
            throw e;
        } finally {
            try {in.close();} catch (Exception e1) {}
            try {out.close();} catch (Exception e1) {}                              
        }
    }

    void copyStream(InputStream in, OutputStream out) throws Exception {
        try {
            // int c = 0;
            int l;
            while ((l = in.read(buf)) > 0) {
                out.write(buf, 0, l);
                // c += l;
            }
        } catch (Exception e) {
            throw e;
        } finally {
            try {
                in.close();
            } catch (Exception e) {}
            try {
                out.close();
            } catch (Exception e) {}
        }                       
    }

}
