package org.openl.gen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.gen.writers.BeanByteCodeWriter;
import org.openl.gen.writers.ConstructorWithParametersWriter;
import org.openl.gen.writers.DefaultConstructorWriter;
import org.openl.gen.writers.EqualsWriter;
import org.openl.gen.writers.GettersWriter;
import org.openl.gen.writers.HashCodeWriter;
import org.openl.gen.writers.SettersWriter;
import org.openl.gen.writers.ToStringWriter;

/**
 * Generates byte code for simple java bean.
 * 
 * @author DLiauchuk
 *
 */
public class POJOByteCodeGenerator {

    private final String beanNameWithPackage;
    private final Class<?> parentClass;
    private Map<String, FieldDescription> beanFields;
    private List<BeanByteCodeWriter> writers;

    /**
     * 
     * @param beanName name of the generated class, with namespace (e.g. <code>my.test.TestClass</code>)
     * @param beanFields map of fields, field name as a key, and type as value.
     * @param parentClass parent class
     * @param additionalConstructor true if required to generate constructor with parameter
     */
    protected POJOByteCodeGenerator(String beanName,
            Map<String, FieldDescription> beanFields,
            Class<?> parentClass,
            Map<String, FieldDescription> parentFields,
            boolean additionalConstructor) {

        this.beanFields = beanFields;
        this.parentClass = parentClass;
        this.beanNameWithPackage = beanName.replace('.', '/');
        LinkedHashMap<String, FieldDescription> allFields = new LinkedHashMap<>();
        allFields.putAll(parentFields);
        allFields.putAll(beanFields);

        this.writers = new ArrayList<>();
        writers.add(new DefaultConstructorWriter(beanNameWithPackage, parentClass, beanFields));
        if (additionalConstructor && allFields.size() < 256) {
            // Generate constructor with parameters only in case where there are
            // less than 256 arguments.
            // 255 arguments to the method is a Java limitation
            //
            writers.add(new ConstructorWithParametersWriter(beanNameWithPackage,
                parentClass,
                beanFields,
                parentFields,
                allFields));
        }
        writers.add(new GettersWriter(beanNameWithPackage, beanFields));
        writers.add(new SettersWriter(beanNameWithPackage, beanFields));
        writers.add(new ToStringWriter(beanNameWithPackage, allFields));
        writers.add(new EqualsWriter(beanNameWithPackage, allFields));
        writers.add(new HashCodeWriter(beanNameWithPackage, allFields));
    }

    private void visitClassDescription(ClassWriter classWriter) {
        String parent = Type.getInternalName(parentClass);
        classWriter.visit(Opcodes.V1_6,
            Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER,
            beanNameWithPackage,
            null,
            parent,
            getDefaultInterfaces());
    }

    protected String[] getDefaultInterfaces() {
        return new String[] { "java/io/Serializable" };
    }

    private void visitJAXBAnnotation(ClassWriter classWriter) {
        String namespace = getNamespace(beanNameWithPackage);
        String name = beanNameWithPackage.substring(beanNameWithPackage.lastIndexOf('/') + 1);

        AnnotationVisitor av = classWriter.visitAnnotation("Ljavax/xml/bind/annotation/XmlRootElement;", true);
        av.visit("namespace", namespace);
        av.visit("name", name);
        av.visitEnd();

        av = classWriter.visitAnnotation("Ljavax/xml/bind/annotation/XmlType;", true);
        av.visit("namespace", namespace);
        av.visit("name", name);
        av.visitEnd();
    }

    private void visitFields(ClassWriter classWriter) {
        for (Map.Entry<String, FieldDescription> field : beanFields.entrySet()) {
            String fieldTypeName = field.getValue().getTypeDescriptor();
            classWriter.visitField(Opcodes.ACC_PROTECTED, field.getKey(), fieldTypeName, null, null);
        }
    }

    private static String getNamespace(String beannameWithPackage) {
        String[] parts = beannameWithPackage.split("/");
        StringBuilder builder = new StringBuilder("http://");
        for (int i = parts.length - 2; i >= 0; i--) {
            builder.append(parts[i]);
            if (i != 0) {
                builder.append(".");
            }
        }
        return builder.toString();
    }

    protected void visitExtraByteCodeGeneration(ClassWriter classWriter) {

    }

    /**
     * Return loaded to classpath class object
     *
     * @return <code>Class<?></code> descriptor for given byteCode
     */
    public byte[] byteCode() {
        /* generate byte code */
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        visitClassDescription(classWriter);
        visitJAXBAnnotation(classWriter);
        visitFields(classWriter);

        for (BeanByteCodeWriter writer : writers) {
            writer.write(classWriter);
        }

        visitExtraByteCodeGeneration(classWriter);

        return classWriter.toByteArray();
    }

    protected Map<String, FieldDescription> getBeanFields() {
        return beanFields;
    }

    protected String getBeanNameWithPackage() {
        return beanNameWithPackage;
    }
}