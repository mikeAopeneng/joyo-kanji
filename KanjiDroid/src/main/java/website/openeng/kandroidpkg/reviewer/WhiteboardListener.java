package website.openeng.kandroidpkg.reviewer;

import java.util.EventListener;

public interface WhiteboardListener extends EventListener {
    abstract void onShowWhiteboard();
    abstract void onHideWhiteboard();
}
