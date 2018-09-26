package org.eclipse.jdt.internal.compiler.ast;

public class EnsuresClause extends MethodSpecificationClause {
	
	public EnsuresClause(Expression expression, int sourceStart) {
		super(expression, sourceStart);
	}

	@Override
	public StringBuffer print(int indent, StringBuffer output) {
		printIndent(indent, output);
		output.append("ensures "); //$NON-NLS-1$
		this.expression.print(0, output);
		output.append(";"); //$NON-NLS-1$
		return output;
	}

}
