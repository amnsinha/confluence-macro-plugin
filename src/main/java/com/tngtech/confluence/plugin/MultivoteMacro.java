package com.tngtech.confluence.plugin;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.macro.annotation.Format;
import com.atlassian.confluence.content.render.xhtml.macro.annotation.RequiresFormat;
import com.atlassian.confluence.macro.Macro;
import com.atlassian.confluence.macro.MacroExecutionException;
import org.apache.log4j.Logger;

import java.util.Map;

public class MultivoteMacro implements Macro {
    private static final Logger log = Logger.getLogger(MultivoteMacro.class);

    private final MultiVoteMacroService macroService;

    public MultivoteMacro(MultiVoteMacroService macroService) {
        this.macroService = macroService;
    }

    @Override
    public BodyType getBodyType() {
        return BodyType.RICH_TEXT;
    }

    @Override
    public OutputType getOutputType() {
        return OutputType.BLOCK;
    }

    @Override
    @RequiresFormat(value = Format.View)
    public String execute(Map<String, String> parameters, String body, ConversionContext context)
            throws MacroExecutionException {
        try {
            return macroService.execute(parameters, body, context.getPageContext());
        } catch (Exception e) {
            log.debug(e);
            throw new MacroExecutionException(e.getMessage());
        }
    }

}
