package com.rocketclient;

import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/**
 * Renders a live, rotatable Minecraft skin + cape preview using skinview3d
 * (https://github.com/bs-community/skinview3d) loaded from CDN inside a WebView.
 * This replaces an earlier hand-built JavaFX 3D box model that had rendering bugs -
 * skinview3d is the same renderer NameMC and most skin sites use, with correct UV
 * mapping and mouse-drag orbit controls built in, so there's no custom geometry to
 * get wrong here.
 */
public class SkinViewerWeb extends StackPane {

    private final WebView webView = new WebView();
    private final WebEngine engine = webView.getEngine();
    private final double width;
    private final double height;

    public SkinViewerWeb(double width, double height) {
        this.width = width;
        this.height = height;
        webView.setPrefSize(width, height);
        webView.setContextMenuEnabled(false);
        webView.setStyle("-fx-background-color: transparent;");
        getChildren().add(webView);
        engine.loadContent(shellHtml(), "text/html");
    }

    /** Loads (or reloads) the preview with the given skin/cape textures and body model. */
    public void setSkin(String skinUrl, String capeUrl, boolean slim) {
        String capeOpt = capeUrl != null ? (", cape: '" + escape(capeUrl) + "'") : "";
        String html =
            "<html><head><style>" +
            "html,body{margin:0;padding:0;background:#141414;overflow:hidden;}" +
            "#msg{color:#f44336;font-family:monospace;font-size:11px;padding:8px;}" +
            "</style></head><body>" +
            "<canvas id=\"c\" width=\"" + (int) width + "\" height=\"" + (int) height + "\"></canvas>" +
            "<div id=\"msg\"></div>" +
            "<script src=\"https://cdn.jsdelivr.net/npm/skinview3d/dist/skinview3d.bundle.js\"></script>" +
            "<script>" +
            "try {" +
            "  var viewer = new skinview3d.SkinViewer({" +
            "    canvas: document.getElementById('c')," +
            "    width: " + (int) width + ", height: " + (int) height + "," +
            "    skin: '" + escape(skinUrl) + "'," +
            "    model: '" + (slim ? "slim" : "default") + "'" +
            capeOpt +
            "  });" +
            "  viewer.background = 0x141414;" +
            "  viewer.autoRotate = false;" +
            "  viewer.zoom = 0.85;" +
            // built-in OrbitControls already handle drag-to-rotate + scroll-to-zoom
            "} catch (e) {" +
            "  document.getElementById('msg').textContent = 'Could not load 3D preview: ' + e.message;" +
            "}" +
            "</script></body></html>";
        engine.loadContent(html, "text/html");
    }

    private String shellHtml() {
        return "<html><body style=\"margin:0;background:#141414;\"></body></html>";
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("'", "\\'");
    }
}
