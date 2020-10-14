package ru.dimock.arearth;

public class ArEarthInputManager {
    private static int [][] bplaceIds = {
            {R.drawable.add_earth_button_grey, R.drawable.add_earth_button_grey},
            {R.drawable.add_earth_button, R.drawable.add_earth_button_pressed}};

    private static int [][] bremoveIds = {
            {R.drawable.remove_earth_button_grey, R.drawable.remove_earth_button_grey},
            {R.drawable.remove_earth_button, R.drawable.remove_earth_button_pressed}};

    private static int [][] bqueryIds = {
            {R.drawable.query_button_grey, R.drawable.query_button_grey},
            {R.drawable.query_button, R.drawable.query_button_pressed}};

    private static int [][] bcloseIds = {
            {R.drawable.close_button_grey, R.drawable.close_button_grey},
            {R.drawable.close_button, R.drawable.close_button_pressed}};

    private ArEarthRenderMode renderMode = ArEarthRenderMode.ARM_SPLASH_SCREEN;
    private ArEarthRenderMode savedMode = ArEarthRenderMode.ARM_SPLASH_SCREEN;
    private ArEarthButton buttonPlace;
    private ArEarthButton buttonRemove;
    private ArEarthButton buttonHelp;
    private ArEarthButton buttonClose;
    private ArEarthActivity context;
    private InputManagerCallback icallback;
    private ArEarthSplashScreen splash;
    private ArEarthFont font;

    private Vector3 textPos = new Vector3();

    private static float bigLetterSize = 0.12f;
    private static float mediumLetterSize = 0.085f;
    private static float smallLetterSize = 0.042f;
    private static float helpLetterSize = 0.05f;
    private static float glineSpacing = 0.02f;
    private static float glogoPosY = 0.2f;
    private static float ghelpStartPosY = 0.4f;
    private static float buttonHeight = 0.05f;
    private static float buttonMargin = 0.05f;
    private static float helpTextMargin = 0.01f;

    private Vector3 buttonPlacePos;
    private Vector3 buttonRemovePos;
    private Vector3 buttonHelpPos;
    private Vector3 buttonClosePos;
    private Vector3 buttonSize;
    private Vector3 buttonOffset;

    public ArEarthInputManager(ArEarthActivity c, InputManagerCallback cbk) {
        context = c;
        icallback = cbk;
        buttonPlace = new ArEarthButton(context, bplaceIds);
        buttonRemove = new ArEarthButton(context, bremoveIds);
        buttonHelp = new ArEarthButton(context, bqueryIds);
        buttonClose = new ArEarthButton(context, bcloseIds);
        splash = new ArEarthSplashScreen(context);
        font = new ArEarthFont(context,16, FontStyle.Normal);
        context.runOnUiThread(new Runnable() {
            public void run() {
                context.startSplash();
            }
        });
    }

    public void updateViewport(int width, int height) {
        float xdpi = context.getResources().getDisplayMetrics().xdpi;
        float ydpi = context.getResources().getDisplayMetrics().ydpi;
        float ratio = (height * xdpi) / (width * ydpi);
        font.updateViewportRatio(ratio);
        buttonSize = new Vector3(buttonHeight*ratio, buttonHeight, 1);
        buttonOffset = new Vector3(buttonMargin*ratio, buttonMargin, 0);
        buttonPlacePos = new Vector3(1.0f - buttonOffset.x() - buttonSize.x(), -1.0f + buttonOffset.y() + buttonSize.y(), 0);
        buttonRemovePos = new Vector3(-1.0f + buttonOffset.x() + buttonSize.x(), -1.0f + buttonOffset.y() + buttonSize.y(), 0);
        buttonHelpPos = new Vector3(-1.0f + buttonOffset.x() + buttonSize.x(), 1.0f - buttonOffset.y() - buttonSize.y(), 0);
        buttonClosePos = new Vector3(1.0f - buttonOffset.x() - buttonSize.x(), 1.0f - buttonOffset.y() - buttonSize.y(), 0);
        buttonPlace.setPosition(buttonPlacePos, buttonSize);
        buttonRemove.setPosition(buttonRemovePos, buttonSize);
        buttonClose.setPosition(buttonClosePos, buttonSize);
        buttonHelp.setPosition(buttonHelpPos, buttonSize);
        buttonHelp.setEnabled(true);
        buttonClose.setEnabled(true);
    }

    public ArEarthRenderMode getRenderMode() {
        return renderMode;
    }

    public void draw() {
        buttonPlace.draw();
        buttonRemove.draw();
        buttonHelp.draw();
        buttonClose.draw();
    }

    public void drawHelpScreen() {
        buttonPlace.setEnabled(true);
        buttonRemove.setEnabled(true);
        draw();
        buttonPlace.setEnabled(false);
        buttonRemove.setEnabled(false);

        textPos.assign(buttonPlacePos);
        textPos.sety(textPos.y());
        textPos.setx(textPos.x() - buttonSize.x() - helpTextMargin);
        font.draw(textPos, context.getResources().getString(R.string.place_earth), helpLetterSize, TextAlign.Right, TextAlign.Center);

        textPos.assign(buttonRemovePos);
        textPos.sety(textPos.y());
        textPos.setx(textPos.x() + buttonSize.x() + helpTextMargin);
        font.draw(textPos, context.getResources().getString(R.string.remove_earth), helpLetterSize, TextAlign.Left, TextAlign.Center);

        textPos.assign(buttonHelpPos);
        textPos.sety(textPos.y());
        textPos.setx(textPos.x() + buttonSize.x() + helpTextMargin);
        font.draw(textPos, context.getResources().getString(R.string.show_help), helpLetterSize, TextAlign.Left, TextAlign.Center);

        textPos.assign(buttonClosePos);
        textPos.sety(textPos.y());
        textPos.setx(textPos.x() - buttonSize.x() - helpTextMargin);
        font.draw(textPos, context.getResources().getString(R.string.exit_id), helpLetterSize, TextAlign.Right, TextAlign.Center);

        float y = drawContinueMessage(ghelpStartPosY);
        y -= bigLetterSize;
        y = drawAppNameAndAuthor(y);
        y = drawSourceCodeMessage(y - glineSpacing);
        y = drawAttributions(y - glineSpacing);
        drawArCoreMessage(y - glineSpacing);
    }

    public void drawSplashScreen() {
        if(context.splashCompleted()) {
            renderMode = ArEarthRenderMode.ARM_NOT_READY;
            onModeChange(renderMode);
            icallback.onInput(renderMode);
            return;
        }
        splash.draw(null, null, null, null, null, null);
        drawSplashScreenText();
    }

    public void drawPausedScreen() {
        textPos.setx(0);
        textPos.sety(0);
        font.draw(textPos, context.getResources().getString(R.string.tracking_is_paused), mediumLetterSize, TextAlign.Center, TextAlign.Center);
        buttonClose.draw();
    }

    public void drawNotReadyScreen() {
        textPos.setx(0);
        textPos.sety(0);
        font.draw(textPos, context.getResources().getString(R.string.waiting_for_tracking), mediumLetterSize, TextAlign.Center, TextAlign.Center);
        buttonClose.draw();
    }

    private float drawContinueMessage(float y) {
        textPos.setx(0);
        textPos.sety(y);
        font.draw(textPos, context.getResources().getString(R.string.tap_to_continue), mediumLetterSize, TextAlign.Center, TextAlign.Bottom);
        y -= mediumLetterSize;
        return y;
    }

    private float drawAttributions(float y) {
        textPos.setx(0);
        textPos.sety(y);
        font.draw(textPos, context.getResources().getString(R.string.solar_system_1), smallLetterSize, TextAlign.Center, TextAlign.Bottom);
        y -= smallLetterSize;

        textPos.sety(y);
        font.draw(textPos, context.getResources().getString(R.string.solar_system_2), smallLetterSize, TextAlign.Center, TextAlign.Bottom);
        y -= smallLetterSize;

        textPos.sety(y);
        font.draw(textPos, context.getResources().getString(R.string.solar_system_3), smallLetterSize, TextAlign.Center, TextAlign.Bottom);
        y -= smallLetterSize;

        textPos.sety(y);
        font.draw(textPos, context.getResources().getString(R.string.solar_system_4), smallLetterSize, TextAlign.Center, TextAlign.Bottom);
        y -= 2*smallLetterSize;

        textPos.sety(y);
        font.draw(textPos, context.getResources().getString(R.string.nasa_reference_1), smallLetterSize, TextAlign.Center, TextAlign.Bottom);
        y -= smallLetterSize;

        textPos.sety(y);
        font.draw(textPos, context.getResources().getString(R.string.nasa_reference_2), smallLetterSize, TextAlign.Center, TextAlign.Bottom);
        y -= smallLetterSize;

        textPos.sety(y);
        font.draw(textPos, context.getResources().getString(R.string.nasa_reference_3), smallLetterSize, TextAlign.Center, TextAlign.Bottom);
        y -= smallLetterSize;

        return y;
    }

    private float drawArCoreMessage(float y) {
        textPos.sety(y);
        font.draw(textPos, context.getResources().getString(R.string.splash_screen_arcore_1), smallLetterSize, TextAlign.Center, TextAlign.Bottom);
        y -= smallLetterSize;

        textPos.sety(y);
        font.draw(textPos,context.getResources().getString(R.string.splash_screen_arcore_2), smallLetterSize, TextAlign.Center, TextAlign.Bottom);
        y -= smallLetterSize;

        textPos.sety(y);
        font.draw(textPos,context.getResources().getString(R.string.splash_screen_arcore_3), smallLetterSize, TextAlign.Center, TextAlign.Bottom);
        y -= smallLetterSize;

        return y;
    }

    private float drawAppNameAndAuthor(float y) {
        textPos.setx(0);
        textPos.sety(y);
        font.draw(textPos, context.getResources().getString(R.string.app_name), bigLetterSize, TextAlign.Center, TextAlign.Bottom);
        y -= bigLetterSize;

        String versionName = "(version " + BuildConfig.VERSION_NAME + ")";
        textPos.sety(y);
        font.draw(textPos, versionName, smallLetterSize, TextAlign.Center, TextAlign.Bottom);
        y -= 2*smallLetterSize;

        textPos.sety(y);
        font.draw(textPos, context.getResources().getString(R.string.author_id), smallLetterSize, TextAlign.Center, TextAlign.Bottom);
        y -= smallLetterSize;

        textPos.sety(y);
        font.draw(textPos, context.getResources().getString(R.string.mit_license), smallLetterSize, TextAlign.Center, TextAlign.Bottom);
        y -= smallLetterSize;

        return y;
    }

    private float drawPressHelpMessage(float y) {
        textPos.setx(0);
        textPos.sety(y);
        font.draw(textPos, context.getResources().getString(R.string.press_help_id), smallLetterSize, TextAlign.Center, TextAlign.Bottom);
        y -= smallLetterSize;
        return y;
    }

    private float drawSourceCodeMessage(float y) {
        textPos.setx(0);
        textPos.sety(y);
        font.draw(textPos, context.getResources().getString(R.string.source_code_id_1), smallLetterSize, TextAlign.Center, TextAlign.Bottom);
        y -= smallLetterSize;

        textPos.sety(y);
        font.draw(textPos, context.getResources().getString(R.string.source_code_id_2), smallLetterSize, TextAlign.Center, TextAlign.Bottom);
        y -= smallLetterSize;
        return y;
    }

    private void drawSplashScreenText() {
        float y = drawAppNameAndAuthor(glogoPosY);
        y = drawPressHelpMessage(y - glineSpacing);
        y = drawSourceCodeMessage(y - glineSpacing);
        y = drawAttributions(y - glineSpacing);
        drawArCoreMessage(y - glineSpacing);
    }

    public void onModeChange(ArEarthRenderMode rmode) {
        renderMode = rmode;
        switch(renderMode) {
            case ARM_PAUSED:
            case ARM_NOT_READY:
            case ARM_SHOW_HELP: {
                buttonPlace.setEnabled(false);
                buttonRemove.setEnabled(false);
                break;
            }
            case ARM_POSITIONING: {
                buttonPlace.setEnabled(true);
                buttonRemove.setEnabled(false);
                break;
            }
            case ARM_NORMAL_RENDER: {
                buttonPlace.setEnabled(false);
                buttonRemove.setEnabled(true);
                break;
            }
        }
    }

    public void onDown(Vector3 p) {
        buttonClose.onButtonDown(p);
        switch(renderMode) {
            case ARM_POSITIONING: {
                buttonPlace.onButtonDown(p);
                buttonHelp.onButtonDown(p);
                break;
            }
            case ARM_NORMAL_RENDER: {
                buttonRemove.onButtonDown(p);
                buttonHelp.onButtonDown(p);
                break;
            }
            case ARM_NOT_READY: {
                buttonHelp.onButtonDown(p);
                break;
            }
            case ARM_SHOW_HELP: {
                onModeChange(savedMode);
                icallback.onInput(renderMode);
                break;
            }
        }
    }

    public void onUp(Vector3 p) {
        if(buttonClose.onButtonUp(p)) {
            renderMode = ArEarthRenderMode.ARM_CLOSE;
            icallback.onInput(renderMode);
            return;
        }
        switch(renderMode) {
            case ARM_POSITIONING: {
                if (buttonPlace.onButtonUp(p)) {
                    renderMode = ArEarthRenderMode.ARM_NORMAL_RENDER;
                    buttonPlace.setEnabled(false);
                    buttonRemove.setEnabled(true);
                    icallback.onInput(renderMode);
                }
                else if(buttonHelp.onButtonUp(p)) {
                    savedMode = renderMode;
                    renderMode = ArEarthRenderMode.ARM_SHOW_HELP;
                    buttonPlace.setEnabled(false);
                    buttonRemove.setEnabled(false);
                    icallback.onInput(renderMode);
                }
                break;
            }

            case ARM_NORMAL_RENDER: {
                if(buttonRemove.onButtonUp(p)) {
                    renderMode = ArEarthRenderMode.ARM_POSITIONING;
                    buttonPlace.setEnabled(true);
                    buttonRemove.setEnabled(false);
                    icallback.onInput(renderMode);
                }
                else if(buttonHelp.onButtonUp(p)) {
                    savedMode = renderMode;
                    renderMode = ArEarthRenderMode.ARM_SHOW_HELP;
                    buttonPlace.setEnabled(false);
                    buttonRemove.setEnabled(false);
                    icallback.onInput(renderMode);
                }
                break;
            }

            case ARM_NOT_READY: {
                if(buttonHelp.onButtonUp(p)) {
                    savedMode = renderMode;
                    renderMode = ArEarthRenderMode.ARM_SHOW_HELP;
                    buttonPlace.setEnabled(false);
                    buttonRemove.setEnabled(false);
                    icallback.onInput(renderMode);
                }
                break;
            }
        }
    }

    public void onMove(Vector3 p) {
        buttonClose.onButtonMove(p);
        switch(renderMode) {
            case ARM_POSITIONING: {
                buttonPlace.onButtonMove(p);
                break;
            }
            case ARM_NORMAL_RENDER: {
                buttonRemove.onButtonMove(p);
                break;
            }
        }
        buttonHelp.onButtonMove(p);
    }
}
