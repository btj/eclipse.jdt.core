/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.codegen.*;
import org.eclipse.jdt.internal.compiler.flow.*;
import org.eclipse.jdt.internal.compiler.lookup.*;

public class CompoundAssignment extends Assignment implements OperatorIds {
	public int operator;
	public int assignmentImplicitConversion;

	//  var op exp is equivalent to var = (varType) var op exp
	// assignmentImplicitConversion stores the cast needed for the assignment

	public CompoundAssignment(Expression lhs, Expression expression,int operator, int sourceEnd) {
		//lhs is always a reference by construction ,
		//but is build as an expression ==> the checkcast cannot fail
	
		super(lhs, expression, sourceEnd);
		lhs.bits &= ~IsStrictlyAssignedMASK; // tag lhs as NON assigned - it is also a read access
		lhs.bits |= IsCompoundAssignedMASK; // tag lhs as assigned by compound
		this.operator = operator ;
	}
	
	public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {
		// record setting a variable: various scenarii are possible, setting an array reference, 
		// a field reference, a blank final field reference, a field of an enclosing instance or 
		// just a local variable.
	
		return  ((Reference) lhs).analyseAssignment(currentScope, flowContext, flowInfo, this, true).unconditionalInits();
	}
	
	public void generateCode(BlockScope currentScope, CodeStream codeStream, boolean valueRequired) {
	
		// various scenarii are possible, setting an array reference, 
		// a field reference, a blank final field reference, a field of an enclosing instance or 
		// just a local variable.
	
		int pc = codeStream.position;
		 ((Reference) lhs).generateCompoundAssignment(currentScope, codeStream, expression, operator, assignmentImplicitConversion, valueRequired);
		if (valueRequired) {
			codeStream.generateImplicitConversion(implicitConversion);
		}
		codeStream.recordPositionsFrom(pc, this.sourceStart);
	}
	
	public int nullStatus(FlowInfo flowInfo) {
		return FlowInfo.NON_NULL;
	}
	
	public String operatorToString() {
		switch (operator) {
			case PLUS :
				return "+="; //$NON-NLS-1$
			case MINUS :
				return "-="; //$NON-NLS-1$
			case MULTIPLY :
				return "*="; //$NON-NLS-1$
			case DIVIDE :
				return "/="; //$NON-NLS-1$
			case AND :
				return "&="; //$NON-NLS-1$
			case OR :
				return "|="; //$NON-NLS-1$
			case XOR :
				return "^="; //$NON-NLS-1$
			case REMAINDER :
				return "%="; //$NON-NLS-1$
			case LEFT_SHIFT :
				return "<<="; //$NON-NLS-1$
			case RIGHT_SHIFT :
				return ">>="; //$NON-NLS-1$
			case UNSIGNED_RIGHT_SHIFT :
				return ">>>="; //$NON-NLS-1$
		}
		return "unknown operator"; //$NON-NLS-1$
	}
	
	public StringBuffer printExpressionNoParenthesis(int indent, StringBuffer output) {
	
		lhs.printExpression(indent, output).append(' ').append(operatorToString()).append(' ');
		return expression.printExpression(0, output) ; 
	}
	
	public TypeBinding resolveType(BlockScope scope) {
		constant = NotAConstant;
		if (!(this.lhs instanceof Reference) || this.lhs.isThis()) {
			scope.problemReporter().expressionShouldBeAVariable(this.lhs);
			return null;
		}
		TypeBinding lhsType = lhs.resolveType(scope);
		TypeBinding expressionType = expression.resolveType(scope);
		if (lhsType == null || expressionType == null)
			return null;
	
		int lhsID = lhsType.id;
		int expressionID = expressionType.id;
		
		// autoboxing support
		boolean use15specifics = scope.environment().options.sourceLevel >= JDK1_5;
		boolean unboxedLhs = false, unboxedExpression = false;
		if (use15specifics) {
			if (!lhsType.isBaseType() && expressionID != T_JavaLangString) {
				int unboxedID = scope.computeBoxingType(lhsType).id;
				if (unboxedID != lhsID) {
					lhsID = unboxedID;
					unboxedLhs = true;
				}
			}
			if (!expressionType.isBaseType() && lhsID != T_JavaLangString) {
				int unboxedID = scope.computeBoxingType(expressionType).id;
				if (unboxedID != expressionID) {
					expressionID = unboxedID;
					unboxedExpression = true;
				}
			}
		}
		
		if (restrainUsageToNumericTypes() && !lhsType.isNumericType()) {
			scope.problemReporter().operatorOnlyValidOnNumericType(this, lhsType, expressionType);
			return null;
		}
		if (lhsID > 15 || expressionID > 15) {
			if (lhsID != T_JavaLangString) { // String += Thread is valid whereas Thread += String  is not
				scope.problemReporter().invalidOperator(this, lhsType, expressionType);
				return null;
			}
			expressionID = T_JavaLangObject; // use the Object has tag table
		}
	
		// the code is an int
		// (cast)  left   Op (cast)  rigth --> result 
		//  0000   0000       0000   0000      0000
		//  <<16   <<12       <<8     <<4        <<0
	
		// the conversion is stored INTO the reference (info needed for the code gen)
		int result = OperatorExpression.OperatorSignatures[operator][ (lhsID << 4) + expressionID];
		if (result == T_undefined) {
			scope.problemReporter().invalidOperator(this, lhsType, expressionType);
			return null;
		}
		if (operator == PLUS){
			if(lhsID == T_JavaLangObject) {
				// <Object> += <String> is illegal (39248)
				scope.problemReporter().invalidOperator(this, lhsType, expressionType);
				return null;
			} else {
				// <int | boolean> += <String> is illegal
				if ((lhsType.isNumericType() || lhsID == T_boolean) && !expressionType.isNumericType()){
					scope.problemReporter().invalidOperator(this, lhsType, expressionType);
					return null;
				}
			}
		}
		this.lhs.implicitConversion = (unboxedLhs ? UNBOXING : 0) | (result >>> 12);
		this.expression.implicitConversion = (unboxedExpression ? UNBOXING : 0) | ((result >>> 4) & 0x000FF);
		this.assignmentImplicitConversion =  (unboxedLhs ? BOXING : 0) | (lhsID << 4) | (result & 0x0000F);
		return this.resolvedType = lhsType;
	}
	
	public boolean restrainUsageToNumericTypes(){
		return false ;
	}
	
	public void traverse(ASTVisitor visitor, BlockScope scope) {
		if (visitor.visit(this, scope)) {
			lhs.traverse(visitor, scope);
			expression.traverse(visitor, scope);
		}
		visitor.endVisit(this, scope);
	}
}
