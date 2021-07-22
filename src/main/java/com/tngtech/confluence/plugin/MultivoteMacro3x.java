package com.tngtech.confluence.plugin;

import com.atlassian.renderer.RenderContext;
import com.atlassian.renderer.v2.RenderMode;
import com.atlassian.renderer.v2.macro.BaseMacro;
import com.atlassian.renderer.v2.macro.MacroException;
import org.apache.log4j.Logger;

import java.util.Map;

public class MultivoteMacro3x extends BaseMacro {
    private static final Logger log = Logger.getLogger(MultivoteMacro3x.class);

    private final MultiVoteMacroService macroService;

    public MultivoteMacro3x(MultiVoteMacroService macroService) {
        this.macroService = macroService;
    }

    @Override
    public boolean isInline() {
        return false;
    }

    @Override
    public boolean hasBody() {
        return true;
    }

    @Override
    public RenderMode getBodyRenderMode() {
        return RenderMode.ALL;
    }

    @Override
    public String execute(Map params, String body, RenderContext renderContext) throws MacroException {
        try {
            return macroService.execute(params, body, renderContext);
        } catch (Exception e) {
            log.debug(e);
            throw new MacroException(e.getMessage());
        }
    }

}
