package org.eclipse.jdt.core;

import org.eclipse.jdt.internal.compiler.batch.Main;

/**
 * @since 3.21
 */
public class s4jie2TestSuite {
	
	public static void assertTrue(boolean b) { if (!b) throw new AssertionError(); }
	public static void assertFalse(boolean b) { assertTrue(!b); }
	
	public static void assertEquals(boolean actual, boolean expected) { assertTrue(actual == expected); }
	
	public static void test(String filename, boolean expectedSuccess) {
		System.out.println("Test " + filename + " start");
		assertEquals(Main.compile("-source 1.8 -proc:none s4jie2-tests/src/" + filename + ".java -d s4jie2-tests/bin"), expectedSuccess);
		System.out.println("Test " + filename + " success");
	}
	
	public static void main(String[] args) throws Exception {
		test("Minimal", true);
		
		test("GameCharacter_pre", true);
		ProcessBuilder builder = new ProcessBuilder("D:/jdk1.8.0_231/bin/java", "-classpath", "s4jie2-tests/bin;D:/s4jie2/codespecs/codespecs.jar", "GameCharacter_pre");
		builder.inheritIO();
		System.out.println("GameCharacter_pre execution terminated with exit code " + builder.start().waitFor());

		test("GameCharacter_pre_fail", false);
		test("GameCharacter_pre_type_error", false);
		
		test("GameCharacter_pre_post", true);
		test("GameCharacter_pre_post_syntax_error", false);
		
		System.out.println("s4jie2TestSuite: All tests passed.");
	}

}
