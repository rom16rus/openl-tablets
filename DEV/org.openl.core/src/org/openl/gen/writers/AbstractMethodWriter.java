package org.openl.gen.writers;

import static org.objectweb.asm.Opcodes.ACC_ABSTRACT;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

import java.util.Objects;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.openl.gen.AnnotationDescription;
import org.openl.gen.MethodDescription;
import org.openl.gen.TypeDescription;

/**
 * Writes abstract method with annotations (if presents) to the target {@code class} or {@code interface}
 *
 * @author Vladyslav Pikus
 */
public class AbstractMethodWriter extends ChainedBeanByteCodeWriter {

    private final MethodDescription description;

    /**
     * Initialize method writter with given parameters
     *
     * @param description method description
     * @param next link to the next writter
     */
    public AbstractMethodWriter(MethodDescription description, ChainedBeanByteCodeWriter next) {
        super(next);
        this.description = Objects.requireNonNull(description, "Method description is null.");
    }

    /**
     * Writes method annotations
     *
     * @param mv target method visitor
     */
    private void visitMethodAnnotations(MethodVisitor mv) {
        for (AnnotationDescription annotation : description.getAnnotations()) {
            AnnotationVisitor av = mv.visitAnnotation(annotation.getAnnotationType().getTypeDescriptor(), true);
            for (AnnotationDescription.AnnotationProperty property : annotation.getProperties()) {
                av.visit(property.getName(), property.getValue());
            }
            av.visitEnd();
        }
    }

    /**
     * Writes annotations of method parameters
     *
     * @param mv method visitor
     */
    private void visitMethodParametersAnnotations(MethodVisitor mv) {
        int i = 0;
        for (TypeDescription param : description.getArgsTypes()) {
            for (AnnotationDescription annotation : param.getAnnotations()) {
                AnnotationVisitor av = mv.visitParameterAnnotation(i,
                        annotation.getAnnotationType().getTypeDescriptor(),
                        true);
                for (AnnotationDescription.AnnotationProperty property : annotation.getProperties()) {
                    av.visit(property.getName(), property.getValue());
                }
                av.visitEnd();
            }
            i++;
        }
    }

    /**
     * Writes abstract method
     *
     * @param cw
     */
    @Override
    protected void writeInternal(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC + ACC_ABSTRACT,
                description.getName(),
                buildMethodDescriptor(),
                null,
                null);
        visitMethodAnnotations(mv);
        visitMethodParametersAnnotations(mv);
    }

    /**
     * Creates method descriptor.<br/>
     * <p>
     *     Examples:
     *     <pre>
     *         ()V
     *         (Ljava/lang/Object;ILjava/util/Date;)I
     *     </pre>
     * </p>
     *
     * @return method descriptor
     */
    private String buildMethodDescriptor() {
        StringBuilder builder = new StringBuilder("(");
        for (TypeDescription arg : description.getArgsTypes()) {
            builder.append(arg.getTypeDescriptor());
        }
        builder.append(')').append(description.getReturnType().getTypeDescriptor());
        return builder.toString();
    }
}
