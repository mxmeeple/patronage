package com.meeple.shared;

import static org.lwjgl.assimp.Assimp.*;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.log4j.Logger;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIColor3D;
import org.lwjgl.assimp.AIColor4D;
import org.lwjgl.assimp.AIFace;
import org.lwjgl.assimp.AIMaterial;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIPropertyStore;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIVector3D;
import org.lwjgl.assimp.Assimp;

import com.meeple.citybuild.client.render.ShaderProgramDefinitions;
import com.meeple.citybuild.client.render.ShaderProgramDefinitions.MeshAttributeGenerator;
import com.meeple.citybuild.client.render.ShaderProgramDefinitions.ShaderProgramDefinition_3D_lit_mat;
import com.meeple.citybuild.client.render.structs.Material;
import com.meeple.shared.frame.OGL.ShaderProgram;
import com.meeple.shared.frame.OGL.ShaderProgram.Attribute;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferDataManagementType;
import com.meeple.shared.frame.OGL.ShaderProgram.BufferObject;
import com.meeple.shared.frame.nuklear.IOUtil;

public class ModelLoader {

    private static Logger logger = Logger.getLogger(ModelLoader.class);

    /**
     * Imports all meshes from a file. Powered by AssImp
     * 
     * @param fileLocation      Location of the file, either internal, relayive or
     *                          absolute
     * @param maxModelInstances Maximum instances of a mesh that will be rendered
     * @param postConvertMesh   Consumer that is called after the conversion of
     *                          every mesh
     * @return A map, mesh names to mesh
     * @throws IOException           if file is not found
     * @throws IllegalStateException if AssImp fails for whatever reason
     */
    public static Map<String, ShaderProgramDefinition_3D_lit_mat.Mesh> loadModelFromFile(String fileLocation, int maxModelInstances,
            Consumer<ShaderProgramDefinition_3D_lit_mat.Mesh> postConvertMesh) throws IOException {

        // read the resource into a buffer, and load the scene from the buffer
        ByteBuffer fileContent = IOUtil.ioResourceToByteBuffer(fileLocation, 2048 * 8);
        AIPropertyStore store = aiCreatePropertyStore();
        aiSetImportPropertyInteger(store, AI_CONFIG_PP_SBP_REMOVE,
                Assimp.aiPrimitiveType_LINE | Assimp.aiPrimitiveType_POINT);
        // aiSetImportPropertyInteger(store, AI_CONFIG_PP_FD_CHECKAREA , 0);
        AIScene scene = aiImportFileFromMemoryWithProperties(
                fileContent,
                0 |
                // aiProcess_JoinIdenticalVertices |
                        aiProcess_Triangulate
                        // aiProcessPreset_TargetRealtime_MaxQuality |
                        // | aiProcess_FindDegenerates
                        | aiProcess_GenNormals | aiProcess_FixInfacingNormals | aiProcess_GenSmoothNormals 
                        | aiProcess_RemoveRedundantMaterials
                        // aiProcess_MakeLeftHanded |
                        // aiProcess_ImproveCacheLocality |
                        // | aiProcess_findi
                        | aiProcess_JoinIdenticalVertices | aiProcess_SortByPType,
                (ByteBuffer) null,
                store);
        if (scene == null) {
            throw new IllegalStateException(aiGetErrorString());
        }
        int meshCount = scene.mNumMeshes();
        Map<String, ShaderProgramDefinition_3D_lit_mat.Mesh> meshes = new HashMap<>(meshCount);
        PointerBuffer meshesBuffer = scene.mMeshes();
        for (int i = 0; i < meshCount; ++i) {
            AIMesh aiMesh = AIMesh.create(meshesBuffer.get(i));
            String meshName = aiMesh.mName().dataString();
            logger.trace("Mesh with name: " + meshName + " just been imported");
            ShaderProgramDefinition_3D_lit_mat.Mesh sp_mesh = ShaderProgramDefinitions.collection._3D_lit_mat
                .createMesh(maxModelInstances);
            setupMesh(sp_mesh, aiMesh, maxModelInstances);
            

            meshes.put(meshName, sp_mesh);
            if (postConvertMesh != null) {
                postConvertMesh.accept(sp_mesh);
            }
            aiMesh.clear();
            aiMesh.free();
        }
        aiReleaseImport(scene);
        return meshes;
    }

    private static IntBuffer convertElementBuffer(AIFace.Buffer facesBuffer, int faceCount, int elementCount) {
        IntBuffer elementArrayBufferData = BufferUtils.createIntBuffer(elementCount);
        for (int i = 0; i < faceCount; ++i) {
            AIFace face = facesBuffer.get(i);
            if (face.mNumIndices() != 3) {
                logger.trace("not 3 verts in a face. actually had " + face.mNumIndices());
            } else {
                elementArrayBufferData.put(face.mIndices());
            }
        }
        elementArrayBufferData.flip();
        return elementArrayBufferData;
    }

    /**
     * sets up the mesh with attributes/VBOs and uses the AIMesh data provided
     * 
     * @param aim       mesh data to read from
     * @param maxMeshes maximum instances of the mesh
     * @return Mesh to be rendered with shader program
     */
    private static ShaderProgram.RenderableVAO setupMesh(ShaderProgramDefinition_3D_lit_mat.Mesh mesh, AIMesh aim, long maxMeshes) {
        
        {
            Attribute vertexAttrib = mesh.vertexAttribute;

            AIVector3D.Buffer vertices = aim.mVertices();
            aim.mVertices().free();
            vertices.clear();
            long bufferLen = (long) (AIVector3D.SIZEOF * vertices.remaining());

            vertexAttrib.bufferAddress = vertices.address();
            vertexAttrib.bufferLen = bufferLen;
            vertexAttrib.bufferResourceType = BufferDataManagementType.Address;

        }
        {
            Attribute normalAttrib = mesh.normalAttribute;
            AIVector3D.Buffer normals = aim.mNormals();
            normalAttrib.bufferAddress = normals.address();
            normalAttrib.bufferLen = (long) (AIVector3D.SIZEOF * normals.remaining());
            normalAttrib.bufferResourceType = BufferDataManagementType.Address;

            // mesh2.normalAttribute.bufferLen = bufferLen;
            // mesh2.normalAttribute.bufferResourceType = BufferDataManagementType.Address;

        }
        {
            BufferObject elementAttrib = mesh.elementAttribute;
            // elementAttrib.dataSize = 3;

            AIFace.Buffer facesBuffer = aim.mFaces();
            int faceCount = aim.mNumFaces();
            int elementCount = faceCount * 3;
            elementAttrib.bufferResourceType = BufferDataManagementType.Buffer;

            IntBuffer elementArrayBufferData = convertElementBuffer(facesBuffer, faceCount, elementCount);
            elementAttrib.buffer = elementArrayBufferData;

            mesh.vertexCount = elementCount;
        }
        {
            PointerBuffer colours = aim.mColors();
            if(colours!=null){
                Attribute colourAttribute = MeshAttributeGenerator.generateColourAttribute(maxMeshes);
                
                colourAttribute.bufferAddress = colours.address();
                colourAttribute.bufferLen = (long) (AIColor4D.SIZEOF * aim.mNumVertices());
                colourAttribute.bufferResourceType = BufferDataManagementType.Address;
            }

        }
        return mesh;
    }

    private void setupMaterial(AIMaterial aim){
        Material material = new Material();
        AIColor4D colour = AIColor4D.malloc();
        aiGetMaterialColor(aim, "", 0, 0, colour);
        //material.baseColour = AI_MATKEY_COLOR_AMBIENT
        //aim.mProperties
        material.reflectiveTint.set(0, 0, 0); // AI_MATKEY_COLOR_REFLECTIVE
        material.reflectivityStrength = 0f;// AI_MATKEY_REFLECTIVITY
    }
}
