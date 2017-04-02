package net.flyingff.util;

import java.io.File;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ResourceScanner {

	public static Set<String> getAllClasspathResources(String pkgName) {
		Set<String> resourceses = new TreeSet<>();
		String packageDirName = pkgName.replace('.', '/');
		if (!packageDirName.endsWith("/")) {
			packageDirName += '/';
		}
		try {
			Enumeration<URL> pkgURLs = Thread.currentThread()
					.getContextClassLoader().getResources(packageDirName);
			while (pkgURLs.hasMoreElements()) {
				URL url = pkgURLs.nextElement();
				String protocol = url.getProtocol();
				if ("file".equalsIgnoreCase(protocol)) {
					String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
					findAndAddResourcesesInPackageByFile(packageDirName, filePath, resourceses);
				} else if ("jar".equalsIgnoreCase(protocol)) {
					JarFile jar;
					jar = ((JarURLConnection) url.openConnection()).getJarFile();
					Enumeration<JarEntry> entries = jar.entries();
					while (entries.hasMoreElements()) {
						String entryName = entries.nextElement().getName();
						if (entryName.charAt(0) == '/') {
							entryName = entryName.substring(1);
						}
						if (entryName.startsWith(packageDirName)) {
							resourceses.add(entryName);
						}
					}
				} else {
					throw new RuntimeException("Unknown protocol: " + protocol);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return resourceses;
	}

	public static void findAndAddResourcesesInPackageByFile(String parentResName, String packagePath,
			Set<String> resources) {
		File dir = new File(packagePath);
		if (!dir.exists() || !dir.isDirectory()) {
			return;
		}
		for (File file : dir.listFiles()) {
			String resName = parentResName + file.getName();
			if (file.isDirectory()) {
				resName += '/';
				findAndAddResourcesesInPackageByFile(resName, file.getAbsolutePath(), resources);
			}
			resources.add(resName);
		}
	}

	/*
	public static void main(String[] args) {
		for (String str : getAllClasspathResources("net.flyingff.bsbridge.tessdata")) {
			System.out.println(str + ", " + Thread.currentThread().getContextClassLoader().getResource(str));
		}
		for (String str : getAllClasspathResources("javax.inject")) {
			System.out.println(str + ", " + Thread.currentThread().getContextClassLoader().getResource(str));
		}
	}*/
}
