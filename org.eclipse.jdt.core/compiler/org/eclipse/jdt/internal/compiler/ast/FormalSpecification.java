package org.eclipse.jdt.internal.compiler.ast;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.codegen.CodeStream;
import org.eclipse.jdt.internal.compiler.codegen.Opcodes;
import org.eclipse.jdt.internal.compiler.flow.ExceptionHandlingFlowContext;
import org.eclipse.jdt.internal.compiler.flow.FlowInfo;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodScope;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.problem.AbortMethod;

public class FormalSpecification {

	private static final char[] preconditionAssertionMessage = "Precondition does not hold".toCharArray(); //$NON-NLS-1$

	private static final char[] PRECONDITION_METHOD_NAME_SUFFIX = "$pre".toCharArray();

	public final AbstractMethodDeclaration method;
	public Expression[] preconditions;
	public Expression[] postconditions;
	
	public AssertStatement[] preconditionAssertStatements; 
	public MethodBinding preconditionMethodBinding;
	
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
		int modifiers = this.method instanceof ConstructorDeclaration || this.method.isStatic() ? ClassFileConstants.AccStatic : 0;
		TypeBinding[] parameterTypes;
		if (this.method instanceof ConstructorDeclaration) {
			parameterTypes = new TypeBinding[this.method.binding.parameters.length + 1];
			parameterTypes[0] = this.method.binding.declaringClass;
			System.arraycopy(this.method.binding.parameters, 0, parameterTypes, 1, this.method.binding.parameters.length);
		} else
			parameterTypes = this.method.binding.parameters;
		this.preconditionMethodBinding = new MethodBinding(ClassFileConstants.AccPrivate | ClassFileConstants.AccSynthetic | modifiers,
				CharOperation.concat(this.method.selector, PRECONDITION_METHOD_NAME_SUFFIX),
				TypeBinding.VOID,
				parameterTypes,
				Binding.NO_EXCEPTIONS,
				this.method.scope.enclosingSourceType());
		if (this.preconditions != null) {
			this.preconditionAssertStatements = new AssertStatement[this.preconditions.length];
			for (int i = 0; i < this.preconditions.length; i++) {
				Expression e = this.preconditions[i];
				this.preconditionAssertStatements[i] = new AssertStatement(new StringLiteral(preconditionAssertionMessage, e.sourceStart, e.sourceEnd, 0), e, e.sourceStart);
				this.preconditionAssertStatements[i].resolve(this.method.scope);
			}
		}
//		if (this.postconditions != null)
//			for (Expression e : this.postconditions)
//				e.resolveTypeExpecting(this.method.scope, TypeBinding.BOOLEAN);
	}

	public void generateCode(ClassScope classScope, ClassFile classFile) {
		classFile.codeStream.wideMode = false; // reset wideMode to false
		int problemResetPC = 0;
		CompilationResult unitResult = null;
		int problemCount = 0;
		if (classScope != null) {
			TypeDeclaration referenceContext = classScope.referenceContext;
			if (referenceContext != null) {
				unitResult = referenceContext.compilationResult();
				problemCount = unitResult.problemCount;
			}
		}
		boolean restart = false;
		boolean abort = false;
		// regular code generation
		do {
			try {
				problemResetPC = classFile.contentsOffset;
				this.generateCode(classFile);
				restart = false;
			} catch (AbortMethod e) {
				// a fatal error was detected during code generation, need to restart code gen if possible
				if (e.compilationResult == CodeStream.RESTART_IN_WIDE_MODE) {
					// a branch target required a goto_w, restart code gen in wide mode.
					classFile.contentsOffset = problemResetPC;
					classFile.methodCount--;
					classFile.codeStream.resetInWideMode(); // request wide mode
					// reset the problem count to prevent reporting the same warning twice
					if (unitResult != null) {
						unitResult.problemCount = problemCount;
					}
					restart = true;
				} else if (e.compilationResult == CodeStream.RESTART_CODE_GEN_FOR_UNUSED_LOCALS_MODE) {
					classFile.contentsOffset = problemResetPC;
					classFile.methodCount--;
					classFile.codeStream.resetForCodeGenUnusedLocals();
					// reset the problem count to prevent reporting the same warning twice
					if (unitResult != null) {
						unitResult.problemCount = problemCount;
					}
					restart = true;
				} else {
					restart = false;
					abort = true; 
				}
			}
		} while (restart);
	}
	
	public void generateCode(ClassFile classFile) {
		classFile.generateMethodInfoHeader(this.preconditionMethodBinding);
		int methodAttributeOffset = classFile.contentsOffset;
		int attributeNumber = classFile.generateMethodInfoAttributes(this.preconditionMethodBinding);
		int codeAttributeOffset = classFile.contentsOffset;
		classFile.generateCodeAttributeHeader();
		CodeStream codeStream = classFile.codeStream;
		codeStream.reset(this, classFile);
		// initialize local positions
		this.method.scope.computeLocalVariablePositions(this.method.binding.isStatic() ? 0 : 1, codeStream);

		// arguments initialization for local variable debug attributes
		if (this.method.arguments != null) {
			for (int i = 0, max = this.method.arguments.length; i < max; i++) {
				LocalVariableBinding argBinding;
				codeStream.addVisibleLocalVariable(argBinding = this.method.arguments[i].binding);
				argBinding.recordInitializationStartPC(0);
			}
		}
		if (this.preconditions != null) {
			for (AssertStatement statement : this.preconditionAssertStatements)
				statement.generateCode(this.method.scope, codeStream);
		}
		// if a problem got reported during code gen, then trigger problem method creation
		if (this.method.ignoreFurtherInvestigation) {
			throw new AbortMethod(this.method.scope.referenceCompilationUnit().compilationResult, null);
		}
		codeStream.return_();
		// local variable attributes
		codeStream.exitUserScope(this.method.scope);
		codeStream.recordPositionsFrom(0, this.sourceEnd());
		try {
			classFile.completeCodeAttribute(codeAttributeOffset,this.method.scope);
		} catch(NegativeArraySizeException e) {
			throw new AbortMethod(this.method.scope.referenceCompilationUnit().compilationResult, null);
		}
		attributeNumber++;
		classFile.completeMethodInfo(this.preconditionMethodBinding, methodAttributeOffset, attributeNumber);
	}

	public void analyzeCode(MethodScope scope, ExceptionHandlingFlowContext methodContext, FlowInfo flowInfo) {
		if (this.preconditions != null) {
			for (int i = 0; i < this.preconditions.length; i++)
				flowInfo = this.preconditionAssertStatements[i].analyseCode(scope, methodContext, flowInfo);
		}
	}

	public void generatePreconditionMethodCall(CodeStream codeStream) {
		int pc = codeStream.position;
		if (!this.method.isStatic()) {
			if (this.method instanceof ConstructorDeclaration)
				codeStream.aconst_null();
			else
				codeStream.aload_0();
		}
		if (this.method.arguments != null) {
			for (int i = 0; i < this.method.arguments.length; i++) {
				codeStream.load(this.method.arguments[i].binding);
			}
		}
		codeStream.invoke(this.preconditionMethodBinding.isStatic() ? Opcodes.OPC_invokestatic : Opcodes.OPC_invokevirtual, this.preconditionMethodBinding, null);
		codeStream.recordPositionsFrom(pc, this.method.bodyStart);
	}

	public int sourceStart() {
		if (this.preconditions != null)
			return this.preconditions[0].sourceStart;
		else
			return this.postconditions[0].sourceStart;
	}

	public int sourceEnd() {
		if (this.postconditions != null)
			return this.postconditions[this.postconditions.length - 1].sourceEnd;
		else
			return this.preconditions[this.preconditions.length - 1].sourceEnd;
	}

}
