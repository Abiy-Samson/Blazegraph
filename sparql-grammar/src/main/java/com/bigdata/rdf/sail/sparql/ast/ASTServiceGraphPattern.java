/* Generated By:JJTree: Do not edit this line. ASTServiceGraphPattern.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.bigdata.rdf.sail.sparql.ast;

import java.util.Map;

import com.bigdata.rdf.sail.sparql.ast.ASTOperationContainer;
import com.bigdata.rdf.sail.sparql.ast.Node;
import com.bigdata.rdf.sail.sparql.ast.SimpleNode;
import com.bigdata.rdf.sail.sparql.ast.SyntaxTreeBuilder;
import com.bigdata.rdf.sail.sparql.ast.SyntaxTreeBuilderVisitor;
import com.bigdata.rdf.sail.sparql.ast.VisitorException;

public class ASTServiceGraphPattern extends SimpleNode {

    private boolean silent;

    private String patternString;

    private Map<String, String> prefixDeclarations;

    private String baseURI;

    private int beginTokenLinePos;

    private int beginTokenColumnPos;

    private int endTokenLinePos;

    private int endTokenColumnPos;

    public ASTServiceGraphPattern(int id) {
        super(id);
    }

    public ASTServiceGraphPattern(SyntaxTreeBuilder p, int id) {
        super(p, id);
    }

    /** Accept the visitor. **/
    public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data)
        throws VisitorException
    {
        return visitor.visit(this, data);
    }

    public void setSilent(boolean silent) {
        this.silent = silent;
    }

    public boolean isSilent() {
        return this.silent;
    }

    /**
     * Return the full text image of the SERVICE clause, including the SERVICE
     * keyword and everything through the closing <code>}</code>.
     * 
     * @return Returns the patternString.
     */
    public String getPatternString() {

        if (patternString == null) {

            final ASTOperationContainer parentContainer = (ASTOperationContainer)getParentContainer(this);

            final String sourceString = parentContainer.getSourceString();

            // snip away line until begin token line position
            String substring = sourceString;
            for (int i = 1; i < getBeginTokenLinePos(); i++) {
                substring = substring.substring(substring.indexOf("\n") + 1);
            }
            
            // snip away until begin token column pos
            substring = substring.substring(getBeginTokenColumnPos() - 1);

            // determine part of the query behind the service pattern closing bracket.
            String toTrimSuffix = sourceString;
            for (int i = 1; i < getEndTokenLinePos(); i++) {
                toTrimSuffix = toTrimSuffix.substring(toTrimSuffix.indexOf("\n") + 1);
            }
            toTrimSuffix = toTrimSuffix.substring(getEndTokenColumnPos() - 1);

            // trim off the end
            patternString = substring.substring(0, substring.lastIndexOf(toTrimSuffix) + 1);
        }

        return patternString;
    }

    private Node getParentContainer(Node node) {
        if (node instanceof ASTOperationContainer || node == null) {
            return node;
        }

        return getParentContainer(node.jjtGetParent());
    }

    /**
     * @param prefixDeclarations
     *        The prefixDeclarations to set.
     */
    public void setPrefixDeclarations(Map<String, String> prefixDeclarations) {
        this.prefixDeclarations = prefixDeclarations;
    }

    /**
     * @return Returns the prefixDeclarations.
     */
    public Map<String, String> getPrefixDeclarations() {
        return prefixDeclarations;
    }

    /**
     * @param baseURI
     *        The baseURI to set.
     */
    public void setBaseURI(String baseURI) {
        this.baseURI = baseURI;
    }

    /**
     * @return Returns the baseURI.
     */
    public String getBaseURI() {
        return baseURI;
    }

    /**
     * @param endTokenColumnPos
     *        The endTokenColumnPos to set.
     */
    public void setEndTokenColumnPos(int endTokenColumnPos) {
        this.endTokenColumnPos = endTokenColumnPos;
    }

    /**
     * @return Returns the endTokenColumnPos.
     */
    public int getEndTokenColumnPos() {
        return endTokenColumnPos;
    }

    /**
     * @param endTokenLinePos
     *        The endTokenLinePos to set.
     */
    public void setEndTokenLinePos(int endTokenLinePos) {
        this.endTokenLinePos = endTokenLinePos;
    }

    /**
     * @return Returns the endTokenLinePos.
     */
    public int getEndTokenLinePos() {
        return endTokenLinePos;
    }

    /**
     * @param beginTokenColumnPos
     *        The beginTokenColumnPos to set.
     */
    public void setBeginTokenColumnPos(int beginTokenColumnPos) {
        this.beginTokenColumnPos = beginTokenColumnPos;
    }

    /**
     * @return Returns the beginTokenColumnPos.
     */
    public int getBeginTokenColumnPos() {
        return beginTokenColumnPos;
    }

    /**
     * @param beginTokenLinePos
     *        The beginTokenLinePos to set.
     */
    public void setBeginTokenLinePos(int beginTokenLinePos) {
        this.beginTokenLinePos = beginTokenLinePos;
    }

    /**
     * @return Returns the beginTokenLinePos.
     */
    public int getBeginTokenLinePos() {
        return beginTokenLinePos;
    }

}
/* JavaCC - OriginalChecksum=e2d20966b7012fe20db402efc6b78901 (do not edit this line) */
