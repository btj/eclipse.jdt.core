package org.eclipse.jdt.internal.compiler.ast;

public abstract class MethodSpecificationClause extends ASTNode {
	
	public Expression expression;
	
	public MethodSpecificationClause(Expression expression, int sourceStart) {
		this.sourceStart = sourceStart;
		this.sourceEnd = expression.sourceEnd;
		this.expression = expression;
	}

}
