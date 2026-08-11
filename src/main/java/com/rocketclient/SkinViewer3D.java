package com.rocketclient;

import javafx.application.Platform;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

/**
 * A real, mouse-rotatable 3D preview of a Minecraft skin (+ cape), built out of
 * textured boxes the same way the vanilla model is laid out - not a static image
 * swapped for different angles. Drag horizontally to spin it, drag vertically to
 * tilt. No overlay (hat/jacket/sleeve) layer or arm animation - this is a still
 * pose viewer, not a full player renderer.
 */
public class SkinViewer3D extends StackPane {

    private final Group modelGroup = new Group();
    private final Rotate rotateY = new Rotate(-25, Rotate.Y_AXIS);
    private final Rotate rotateX = new Rotate(-8, Rotate.X_AXIS);
    private double lastMouseX, lastMouseY;

    public SkinViewer3D(double width, double height) {
        setPrefSize(width, height);
        setStyle("-fx-background-color: transparent;");

        modelGroup.getTransforms().addAll(rotateY, rotateX);

        Group scene3dRoot = new Group(modelGroup);
        AmbientLight ambient = new AmbientLight(Color.WHITE); // flat, even lighting - no per-face shading to fight
        scene3dRoot.getChildren().add(ambient);

        SubScene subScene = new SubScene(scene3dRoot, width, height, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.TRANSPARENT);

        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setNearClip(0.1);
        camera.setFarClip(1000);
        camera.setTranslateZ(-90);
        camera.setFieldOfView(30);
        subScene.setCamera(camera);

        subScene.widthProperty().bind(widthProperty());
        subScene.heightProperty().bind(heightProperty());

        subScene.setCursor(javafx.scene.Cursor.OPEN_HAND);
        subScene.setOnMousePressed(e -> {
            lastMouseX = e.getSceneX();
            lastMouseY = e.getSceneY();
            subScene.setCursor(javafx.scene.Cursor.CLOSED_HAND);
        });
        subScene.setOnMouseReleased(e -> subScene.setCursor(javafx.scene.Cursor.OPEN_HAND));
        subScene.setOnMouseDragged(e -> {
            double dx = e.getSceneX() - lastMouseX;
            double dy = e.getSceneY() - lastMouseY;
            rotateY.setAngle(rotateY.getAngle() + dx * 0.5);
            double newPitch = rotateX.getAngle() - dy * 0.5;
            rotateX.setAngle(Math.max(-60, Math.min(60, newPitch))); // clamp so it can't flip upside down
            lastMouseX = e.getSceneX();
            lastMouseY = e.getSceneY();
        });

        getChildren().add(subScene);
    }

    /** Rebuilds the model from a skin texture (64x64) and an optional cape texture (64x32, nullable). */
    public void setSkin(String skinUrl, String capeUrl, boolean slim) {
        Image skinImg = new Image(skinUrl, true);
        skinImg.progressProperty().addListener((obs, oldP, newP) -> {
            if (newP.doubleValue() >= 1.0 && !skinImg.isError()) {
                Platform.runLater(() -> rebuild(skinImg, capeUrl, slim));
            }
        });
        if (skinImg.getProgress() >= 1.0 && !skinImg.isError()) {
            rebuild(skinImg, capeUrl, slim);
        }
    }

    private void rebuild(Image skinImg, String capeUrl, boolean slim) {
        modelGroup.getChildren().clear();

        PhongMaterial skinMat = new PhongMaterial();
        skinMat.setDiffuseMap(skinImg);
        skinMat.setSelfIlluminationMap(skinImg); // fully lit regardless of angle - keeps pixel art crisp/flat

        double armW = slim ? 3 : 4;

        // y grows downward: head 0-8, body 8-20, legs 20-32 (classic Minecraft proportions)
        modelGroup.getChildren().add(box(8, 8, 8, 0, 0, skinImg, skinMat, 0, 4, 0, false));                 // head
        modelGroup.getChildren().add(box(8, 12, 4, 16, 16, skinImg, skinMat, 0, 14, 0, false));              // body
        modelGroup.getChildren().add(box(armW, 12, 4, 40, 16, skinImg, skinMat, -(4 + armW / 2), 14, 0, false)); // right arm
        modelGroup.getChildren().add(box(armW, 12, 4, 40, 16, skinImg, skinMat, (4 + armW / 2), 14, 0, true));   // left arm (mirrored)
        modelGroup.getChildren().add(box(4, 12, 4, 0, 16, skinImg, skinMat, -2, 26, 0, false));              // right leg
        modelGroup.getChildren().add(box(4, 12, 4, 0, 16, skinImg, skinMat, 2, 26, 0, true));                // left leg (mirrored)

        if (capeUrl != null) {
            Image capeImg = new Image(capeUrl, true);
            capeImg.progressProperty().addListener((obs, oldP, newP) -> {
                if (newP.doubleValue() >= 1.0 && !capeImg.isError()) {
                    Platform.runLater(() -> {
                        PhongMaterial capeMat = new PhongMaterial();
                        capeMat.setDiffuseMap(capeImg);
                        capeMat.setSelfIlluminationMap(capeImg);
                        modelGroup.getChildren().add(box(10, 16, 1, 0, 0, capeImg, capeMat, 0, 15, 2.5, false)); // hangs off the back
                    });
                }
            });
        }

        // Recenter roughly around mid-torso so it rotates in place instead of orbiting off-frame.
        modelGroup.getChildren().forEach(n -> n.getTransforms().add(new Translate(0, -16, 0)));
    }

    /**
     * Builds one textured box. (u,v) is the top-left of that part's UV template on the
     * given atlas image, using the standard Minecraft "unwrapped cube" layout: top, bottom,
     * right, front, left, back laid out left-to-right across the texture from that origin.
     */
    private MeshView box(double w, double h, double d, double u, double v, Image atlas, PhongMaterial mat,
                          double cx, double cy, double cz, boolean mirror) {
        double tw = atlas.getWidth();
        double th = atlas.getHeight();

        float x0 = (float) (cx - w / 2), x1 = (float) (cx + w / 2);
        float y0 = (float) (cy - h / 2), y1 = (float) (cy + h / 2);
        float z0 = (float) (cz - d / 2), z1 = (float) (cz + d / 2);

        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().addAll(
            x0, y0, z0,  x1, y0, z0,  x1, y1, z0,  x0, y1, z0, // front face verts 0-3
            x0, y0, z1,  x1, y0, z1,  x1, y1, z1,  x0, y1, z1  // back face verts 4-7
        );

        // UV rects per face, normalized. addUv appends 4 corners (TL,TR,BR,BL) in that order.
        int uvIndex = 0;
        uvIndex = addUv(mesh, u + d, v, w, d, tw, th, mirror);              // top
        int topIdx = uvIndex - 4;
        uvIndex = addUv(mesh, u + d + w, v, w, d, tw, th, mirror);          // bottom
        int bottomIdx = uvIndex - 4;
        uvIndex = addUv(mesh, u, v + d, d, h, tw, th, mirror);              // right
        int rightIdx = uvIndex - 4;
        uvIndex = addUv(mesh, u + d, v + d, w, h, tw, th, mirror);          // front
        int frontIdx = uvIndex - 4;
        uvIndex = addUv(mesh, u + d + w, v + d, d, h, tw, th, mirror);      // left
        int leftIdx = uvIndex - 4;
        uvIndex = addUv(mesh, u + d + w + d, v + d, w, h, tw, th, mirror);  // back
        int backIdx = uvIndex - 4;

        // Faces: each is (point0, uv0, point1, uv1, point2, uv2) per triangle, CCW winding.
        // Each quad below is a rectangle lying entirely on one plane of the box (x0/x1/y0/y1/z0/z1),
        // paired with the UV region that was actually built for that same side.
        addQuadFace(mesh, 0, 1, 2, 3, frontIdx);   // z0 plane - faces the camera
        addQuadFace(mesh, 1, 5, 6, 2, rightIdx);   // x1 plane
        addQuadFace(mesh, 5, 4, 7, 6, backIdx);    // z1 plane
        addQuadFace(mesh, 4, 0, 3, 7, leftIdx);    // x0 plane
        addQuadFace(mesh, 0, 1, 5, 4, topIdx);     // y0 plane
        addQuadFace(mesh, 3, 2, 6, 7, bottomIdx);  // y1 plane

        MeshView view = new MeshView(mesh);
        view.setMaterial(mat);
        view.setCullFace(CullFace.NONE); // texture atlas UVs aren't guaranteed consistent winding per face
        return view;
    }

    private int addUv(TriangleMesh mesh, double px, double py, double pw, double ph, double tw, double th, boolean mirror) {
        float u0 = (float) (px / tw), u1 = (float) ((px + pw) / tw);
        float v0 = (float) (py / th), v1 = (float) ((py + ph) / th);
        if (mirror) { float t = u0; u0 = u1; u1 = t; }
        mesh.getTexCoords().addAll(
            u0, v0,  u1, v0,  u1, v1,  u0, v1
        );
        return mesh.getTexCoords().size() / 2;
    }

    private void addQuadFace(TriangleMesh mesh, int p0, int p1, int p2, int p3, int uvBase) {
        mesh.getFaces().addAll(
            p0, uvBase,     p1, uvBase + 1, p2, uvBase + 2,
            p0, uvBase,     p2, uvBase + 2, p3, uvBase + 3
        );
    }
}
