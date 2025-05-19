package zone.rong.mixinbooter;

import org.objectweb.asm.Opcodes;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.*;

public class MixinBooterClassTransformer implements IClassTransformer {
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (transformedName.equals("net.minecraftforge.fml.common.Loader")) {
            ClassNode classNode = new ClassNode();
            new ClassReader(basicClass).accept(classNode, ClassReader.SKIP_FRAMES);

            for (MethodNode method : classNode.methods) {
                if (method.name.equals("loadMods")) {
                    for (AbstractInsnNode node : method.instructions.toArray()) {
                        if (node instanceof MethodInsnNode && ((MethodInsnNode)node).name.equals("loadData")) {
                            InsnList insnList = new InsnList();

                            insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
                            insnList.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraftforge/fml/common/Loader", "discoverer", "Lnet/minecraftforge/fml/common/discovery/ModDiscoverer;"));
                            insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraftforge/fml/common/discovery/ModDiscoverer", "getASMTable", "()Lnet/minecraftforge/fml/common/discovery/ASMDataTable;", false));
                            insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
                            insnList.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraftforge/fml/common/Loader", "modClassLoader", "Lnet/minecraftforge/fml/common/ModClassLoader;"));
                            insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
                            insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "zone/rong/mixinbooter/LateMixinRedirector", "redirectLateMixins", "(Lnet/minecraftforge/fml/common/discovery/ASMDataTable;Lnet/minecraftforge/fml/common/ModClassLoader;Lnet/minecraftforge/fml/common/Loader;)V", false));

                            method.instructions.insert(node, insnList);
                        }
                    }
                }
            }

            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            classNode.accept(writer);
            return writer.toByteArray();
        }

        return basicClass;
    }
}
