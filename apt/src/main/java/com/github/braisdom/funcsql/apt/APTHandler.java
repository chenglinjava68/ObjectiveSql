package com.github.braisdom.funcsql.apt;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

public final class APTHandler {

    private final JCTree.JCClassDecl classDecl;
    private final Element element;
    private final JCTree ast;
    private final TreeMaker treeMaker;
    private final Names names;
    private final Messager messager;

    APTHandler(JCTree.JCClassDecl classDecl, Element element, JCTree ast, TreeMaker treeMaker,
                      Names names, Messager messager) {
        this.classDecl = classDecl;
        this.element = element;
        this.ast = ast;
        this.treeMaker = treeMaker;
        this.names = names;
        this.messager = messager;
    }

    public MethodBuilder createMethodBuilder() {
        return new MethodBuilder(this);
    }

    public StatementBuilder createBlockBuilder() {
        return new StatementBuilder(this);
    }

    public JCTree get() {
        return ast;
    }

    public String getClassName() {
        return classDecl.name.toString();
    }

    public TreeMaker getTreeMaker() {
        return treeMaker;
    }

    public ElementKind getKind() {
        return this.element.getKind();
    }

    public Name toName(String name) {
        return this.names.fromString(name);
    }

    public JCExpression typeRef(Class clazz) {
        String className = clazz.getName().replace("$", ".");
        return typeRef(className);
    }

    public void inject(JCTree.JCVariableDecl variableDecl) {
        classDecl.defs = classDecl.defs.append(variableDecl);
    }

    public void inject(JCTree.JCMethodDecl methodDecl) {
        classDecl.defs = classDecl.defs.append(methodDecl);
    }

    public JCExpression typeRef(String complexName) {
        String[] parts = complexName.split("\\.");
        if (parts.length > 2 && parts[0].equals("java") && parts[1].equals("lang")) {
            String[] subParts = new String[parts.length - 2];
            System.arraycopy(parts, 2, subParts, 0, subParts.length);
            return javaLangTypeRef(subParts);
        }

        return chainDots(parts);
    }

    public JCExpression javaLangTypeRef(String... simpleNames) {
        return chainDots(null, null, simpleNames);
    }

    public JCExpression chainDots(String elem1, String elem2, String... elems) {
        return chainDots(-1, elem1, elem2, elems);
    }

    public JCExpression chainDots(String... elems) {
        assert elems != null;

        JCExpression e = null;
        for (String elem : elems) {
            if (e == null) e = treeMaker.Ident(toName(elem));
            else e = treeMaker.Select(e, toName(elem));
        }
        return e;
    }

    public JCExpression chainDots(int pos, String elem1, String elem2, String... elems) {
        assert elems != null;
        TreeMaker treeMaker = getTreeMaker();
        if (pos != -1) treeMaker = treeMaker.at(pos);
        JCExpression e = null;
        if (elem1 != null) e = treeMaker.Ident(toName(elem1));
        if (elem2 != null) e = e == null ? treeMaker.Ident(toName(elem2)) : treeMaker.Select(e, toName(elem2));
        for (int i = 0; i < elems.length; i++) {
            e = e == null ? treeMaker.Ident(toName(elems[i])) : treeMaker.Select(e, toName(elems[i]));
        }

        assert e != null;

        return e;
    }

    public JCExpression staticMethodCall(Class<?> clazz, String methodName, JCExpression... params) {
        return treeMaker.Apply(List.nil(), treeMaker.Select(typeRef(clazz.getName()), toName(methodName)), List.from(params));
    }

    public JCExpression newVar(Class<?> clazz, String methodName, JCExpression... params) {
        return treeMaker.Apply(List.nil(), treeMaker.Select(
                typeRef(clazz.getName()), toName(methodName)), List.from(params));
    }

    public JCExpression newGenericsType(Class typeClass, JCExpression... genericTypes) {
        return treeMaker.TypeApply(typeRef(typeClass), List.from(genericTypes));
    }

    public JCExpression newGenericsType(Class typeClass, Class<?>... genericTypeClasses) {
        ListBuffer<JCExpression> genericTypes = new ListBuffer<>();
        for(Class<?> genericTypeClass : genericTypeClasses)
            genericTypes.append(typeRef(genericTypeClass));
        return treeMaker.TypeApply(typeRef(typeClass), genericTypes.toList());
    }

    public JCExpression newGenericsType(Class typeClass, String classSimpleName) {
        return treeMaker.TypeApply(typeRef(typeClass), List.of(treeMaker.Ident(toName(classSimpleName))));
    }


    public JCExpression newArrayType(String typeName) {
        return treeMaker.TypeArray(treeMaker.Ident(toName(typeName)));
    }

    public JCExpression newArrayType(Class typeClass) {
        return treeMaker.TypeArray(typeRef(typeClass));
    }

    public JCExpression newArrayType(JCExpression type) {
        return treeMaker.TypeArray(type);
    }

    public JCExpression varRef(String name) {
        return treeMaker.Ident(toName(name));
    }

    public JCExpression classRef(String name) {
        return treeMaker.Select(treeMaker.Ident(toName(name)), toName("class"));
    }

    public JCExpression classRef(Class<?> clazz) {
        return treeMaker.Select(typeRef(clazz), toName("class"));
    }

}
