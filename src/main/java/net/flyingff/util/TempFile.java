package net.flyingff.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.function.Predicate;


public class TempFile {
	public static File createTempFile(String name) {
		File f = new File(System.getProperty("java.io.tmpdir"), name);
		f.deleteOnExit();
		return f;
	}
	public static File extractTempClasspathResources(String pkgName, String folderName, Predicate<String> acceptor) {
		File tmpFolder = createTempFile(folderName);
		tmpFolder.mkdirs();
		byte[] buf = new byte[65536];
		int lenOfPkgName = pkgName.length();
		for(String str : ResourceScanner.getAllClasspathResources(pkgName)) {
			if(str.endsWith("/")) {
				new File(tmpFolder, str.substring(lenOfPkgName)).mkdirs();
				continue;
			}
			File f = new File(tmpFolder, str.substring(lenOfPkgName));
			try(InputStream is = TempFile.class.getClassLoader().getResourceAsStream(str);
					FileOutputStream fos = new FileOutputStream(f)) {
				int len;
				while((len = is.read(buf)) > 0) {
					fos.write(buf, 0, len);
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			f.deleteOnExit();
		}
		return tmpFolder;
	}
	
	/*public static void main(String[] args) throws Exception{
		File f = extractTempClasspathResources("net.flyingff.bsbridge.tessdata", "x", x->true);
		System.out.println(f);
		Thread.sleep(1000);
	}*/
}
