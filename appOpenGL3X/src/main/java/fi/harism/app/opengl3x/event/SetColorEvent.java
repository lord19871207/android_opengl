package fi.harism.app.opengl3x.event;

public class SetColorEvent {

    private int color;

    public SetColorEvent(int color) {
        this.color = color;
    }

    public int getColor() {
        return color;
    }

}
