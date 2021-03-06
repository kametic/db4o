/* Copyright (C) 2009  Versant Inc.   http://www.db4o.com */
package com.db4o.ta.instrumentation.test;

import java.io.*;
import java.net.*;

import com.db4o.db4ounit.common.ta.*;
import com.db4o.foundation.io.*;
import com.db4o.instrumentation.classfilter.*;
import com.db4o.instrumentation.core.*;
import com.db4o.instrumentation.main.*;
import com.db4o.ta.instrumentation.*;

import db4ounit.*;

public abstract class TAFileEnhancerTestCaseBase implements TestCase, TestLifeCycle {

	protected File srcDir;

	protected abstract Class[] inputClasses();
	protected abstract Class[] instrumentedClasses();

	protected File targetDir;

	public TAFileEnhancerTestCaseBase() {
		super();
	}

	public void setUp() throws Exception {
		srcDir = new File(IO.mkTempDir("tafileinstr/source"));
		targetDir = new File(IO.mkTempDir("tafileinstr/target"));
		copyClassFilesTo(
			inputClasses(),
			srcDir);
	}

	public void tearDown() throws Exception {
		deleteFiles();
	}

	protected AssertingClassLoader newAssertingClassLoader() throws MalformedURLException {
		return newAssertingClassLoader(new Class[0]);
	}

	protected AssertingClassLoader newAssertingClassLoader(Class[] delegatedClasses) throws MalformedURLException {
		return new AssertingClassLoader(targetDir, inputClasses(), delegatedClasses);
	}

	protected void enhance() throws Exception {
		enhance(null);
	}

	protected void enhance(Db4oInstrumentationListener listener) throws Exception {
		Class[] instrumentedClasses = instrumentedClasses();
		String[] filterClassNames = new String[instrumentedClasses.length];
		for (int instrumentedIdx = 0; instrumentedIdx < instrumentedClasses.length; instrumentedIdx++) {
			filterClassNames[instrumentedIdx] = instrumentedClasses[instrumentedIdx].getName();
		}
		ClassFilter filter = new ByNameClassFilter(filterClassNames);
		Db4oFileInstrumentor enhancer = new Db4oFileInstrumentor(new InjectTransparentActivationEdit(filter));
		if(listener != null) {
			enhancer.addInstrumentationListener(listener);
		}
		enhancer.enhance(srcDir, targetDir, new String[]{});
	}

	protected void assertReadsWrites(int expectedReads, int expectedWrites, MockActivator activator) {
		Assert.isGreaterOrEqual(expectedReads, activator.readCount());
		Assert.isGreaterOrEqual(expectedWrites, activator.writeCount());
	}

	private void deleteFiles() {
		deleteDirectory(srcDir.getAbsolutePath());
		deleteDirectory(targetDir.getAbsolutePath());
	}

	private void deleteDirectory(String dirPath) {
		if(!File4.exists(dirPath)) {
			return;
		}
		Directory4.delete(dirPath, true);
	}

	private void copyClassFilesTo(final Class[] classes, final File toDir)
			throws IOException {
				for (int i = 0; i < classes.length; i++) {
					copyClassFile(classes[i], toDir);
				}
			}

	private void copyClassFile(Class clazz, File toDir) throws IOException {
		File file = ClassFiles.fileForClass(clazz);
		String targetPath = Path4.combine(toDir.getAbsolutePath(), ClassFiles.classNameAsPath(clazz));
		File4.delete(targetPath);
		File4.copy(file.getCanonicalPath(), targetPath);
	}

}