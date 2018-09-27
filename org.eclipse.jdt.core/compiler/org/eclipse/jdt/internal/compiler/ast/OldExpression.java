package org.eclipse.jdt.internal.compiler.ast;

import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

public class OldExpression extends Expression {
	
	public Expression expression;
	
	public OldExpression(Expression expression) {
		this.expression = expression;
	}
	
	@Override
	public TypeBinding resolveType(BlockScope scope) {
		this.resolvedType = this.expression.resolveType(scope);
		return this.resolvedType;
	}

	@Override
	public StringBuffer printExpression(int indent, StringBuffer output) {
		output.append("old("); //$NON-NLS-1$
		this.expression.printExpression(0, output);
		output.append(")"); //$NON-NLS-1$
		return output;
	}

}
