package de.sixtyfour.petscii.server;

/**
 *
 *  @author EgonOlsen
 */
public class Parameters {

    private String platform;

    private int colorMapper;

    private boolean backgroundDefault;

    private int background;

    private int prescale;

    private float boost;

    private boolean excludeAlpha;

    private boolean lowerCase;

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public float getBoost() {
        return boost;
    }

    public void setBoost(float boost) {
        this.boost = boost;
    }

    public int getColorMapper() {
        return colorMapper;
    }

    public void setColorMapper(int colorMapper) {
        this.colorMapper = colorMapper;
    }

    public boolean isBackgroundDefault() {
        return backgroundDefault;
    }

    public void setBackgroundDefault(boolean backgroundDefault) {
        this.backgroundDefault = backgroundDefault;
    }

    public int getBackground() {
        return background;
    }

    public void setBackground(int background) {
        this.background = background;
    }

    public int getPrescale() {
        return prescale;
    }

    public void setPrescale(int prescale) {
        this.prescale = prescale;
    }

    public boolean isExcludeAlpha() {
        return excludeAlpha;
    }

    public void setExcludeAlpha(boolean excludeAlpha) {
        this.excludeAlpha = excludeAlpha;
    }

    public boolean isLowerCase() {
        return lowerCase;
    }

    public void setLowerCase(boolean lowerCase) {
        this.lowerCase = lowerCase;
    }
}
