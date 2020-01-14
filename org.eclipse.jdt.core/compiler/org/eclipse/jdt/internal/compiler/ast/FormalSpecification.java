package org.eclipse.jdt.internal.compiler.ast;

import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.codegen.CodeStream;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.SyntheticMethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.problem.AbortMethod;
import org.eclipse.jdt.internal.compiler.problem.AbortType;

public class FormalSpecification {

	public static final char[] SPEC_METHOD_SUFFIX = {'S', 'p', 'e', 'c'};
	
	public final AbstractMethodDeclaration method;
	public Expression[] preconditions;
	public Expression[] postconditions;
	
	public SyntheticMethodBinding binding;

	public FormalSpecification(AbstractMethodDeclaration method) {
		this.method = method;
	}

	public void print(int tab, StringBuffer output) {
		if (this.preconditions != null) {
			for (int i = 0; i < this.preconditions.length; i++) {
				output.append("/** @pre | "); //$NON-NLS-1$
				this.preconditions[i].printExpression(tab, output);
				output.append(" */"); //$NON-NLS-1$
			}
		}
		if (this.postconditions != null) {
			for (int i = 0; i < this.postconditions.length; i++) {
				output.append("/** @post | "); //$NON-NLS-1$
				this.postconditions[i].printExpression(tab, output);
				output.append(" */"); //$NON-NLS-1$
			}
		}
	}

	public void resolve() {
		
		if (this.preconditions != null)
			for (Expression e : this.preconditions)
				e.resolveTypeExpecting(this.method.scope, TypeBinding.BOOLEAN);
//		if (this.postconditions != null)
//			for (Expression e : this.postconditions)
//				e.resolveTypeExpecting(this.method.scope, TypeBinding.BOOLEAN);
	}
	
	public void generateCode() {
		this.binding = this.method.scope.enclosingSourceType().addSyntheticMethod(this);
	}

	public void generateCode(ClassScope classScope, ClassFile classFile) {
		int problemResetPC = 0;
		classFile.codeStream.wideMode = false;
		boolean restart = false;
		do {
			try {
				problemResetPC = classFile.contentsOffset;
				this.generateCode(classFile);
				restart = false;
			} catch (AbortMethod e) {
				// Restart code generation if possible ...
				if (e.compilationResult == CodeStream.RESTART_IN_WIDE_MODE) {
					// a branch target required a goto_w, restart code generation in wide mode.
					classFile.contentsOffset = problemResetPC;
					classFile.methodCount--;
					classFile.codeStream.resetInWideMode(); // request wide mode
					restart = true;
				} else if (e.compilationResult == CodeStream.RESTART_CODE_GEN_FOR_UNUSED_LOCALS_MODE) {
					classFile.contentsOffset = problemResetPC;
					classFile.methodCount--;
					classFile.codeStream.resetForCodeGenUnusedLocals();
					restart = true;
				} else {
					throw new AbortType(this.method.compilationResult, e.problem);
				}
			}
		} while (restart);
	}
	
	public void generateCode(ClassFile classFile) {
		classFile.generateMethodInfoHeader(this.binding);
		int methodAttributeOffset = classFile.contentsOffset;
		int attributeNumber = classFile.generateMethodInfoAttributes(this.binding);
		int codeAttributeOffset = classFile.contentsOffset;
		classFile.generateCodeAttributeHeader();
		CodeStream codeStream = classFile.codeStream;
		codeStream.reset(this, classFile);
		// initialize local positions
		//this.scope.computeLocalVariablePositions(this.outerLocalVariablesSlotSize + (this.binding.isStatic() ? 0 : 1), codeStream);
		// arguments initialization for local variable debug attributes
//		if (this.arguments != null) {
//			for (int i = 0, max = this.arguments.length; i < max; i++) {
//				LocalVariableBinding argBinding;
//				codeStream.addVisibleLocalVariable(argBinding = this.arguments[i].binding);
//				argBinding.recordInitializationStartPC(0);
//			}
//		}
		if (this.preconditions != null) {
			for (Expression expression : this.preconditions) { 
				expression.generateCode(this.method.scope, codeStream, true);
				codeStream.invokeCodespecsRequires();
			}
		}
		codeStream.return_();
		// local variable attributes
		//codeStream.exitUserScope(this.scope);
		//codeStream.recordPositionsFrom(0, this.sourceEnd); // WAS declarationSourceEnd.
		try {
			classFile.completeCodeAttribute(codeAttributeOffset, this.method.scope);
		} catch(NegativeArraySizeException e) {
			throw new AbortMethod(this.method.scope.referenceCompilationUnit().compilationResult, null);
		}
		attributeNumber++;

		classFile.completeMethodInfo(this.binding, methodAttributeOffset, attributeNumber);
	}

}
