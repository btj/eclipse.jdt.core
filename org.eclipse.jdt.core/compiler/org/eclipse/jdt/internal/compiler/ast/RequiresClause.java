package org.eclipse.jdt.internal.compiler.ast;

public class RequiresClause extends MethodSpecificationClause {
	
	public RequiresClause(Expression expression, int sourceStart) {
		super(expression, sourceStart);
	}

	@Override
	public StringBuffer print(int indent, StringBuffer output) {
		printIndent(indent, output);
		output.append("requires "); //$NON-NLS-1$
		this.expression.print(0, output);
		output.append(";"); //$NON-NLS-1$
		return output;
	}

}
