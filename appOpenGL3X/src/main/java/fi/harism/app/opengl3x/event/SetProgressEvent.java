package fi.harism.app.opengl3x.event;

public class SetProgressEvent {

    private int progress;
    private int maxProgress;

    public SetProgressEvent(int progress, int maxProgress) {
        this.progress = progress;
        this.maxProgress = maxProgress;
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return maxProgress;
    }

}
