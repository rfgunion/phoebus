/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newBooleanPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newFilenamePropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newStringPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propBackgroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propBit;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propEnabled;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propFont;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propForegroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propLabelsFromPV;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propOffColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propOffLabel;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propOnColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propOnLabel;

import java.util.Arrays;
import java.util.List;

import org.csstudio.display.builder.model.Messages;
import org.csstudio.display.builder.model.Version;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetConfigurator;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyCategory;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.NamedWidgetColors;
import org.csstudio.display.builder.model.persist.NamedWidgetFonts;
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.csstudio.display.builder.model.persist.XMLUtil;
import org.csstudio.display.builder.model.properties.EnumWidgetProperty;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.properties.WidgetFont;
import org.w3c.dom.Element;

/** Widget that provides button for making a binary change
 *  @author Megan Grodowitz
 */
@SuppressWarnings("nls")
public class BoolButtonWidget extends WritablePVWidget
{
    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
        new WidgetDescriptor("bool_button", WidgetCategory.CONTROL,
            "Boolean Button",
            "platform:/plugin/org.csstudio.display.builder.model/icons/bool_button.png",
            "Button that can toggle one bit of a PV value between 1 and 0",
            Arrays.asList("org.csstudio.opibuilder.widgets.BoolButton",
                          "org.csstudio.opibuilder.widgets.ImageBoolButton",
                          "org.csstudio.opibuilder.widgets.BoolSwitch"))
    {
        @Override
        public Widget createWidget()
        {
            return new BoolButtonWidget();
        }
    };

    /** Handle legacy widged config */
    private static class CustomConfigurator extends WidgetConfigurator
    {
        public CustomConfigurator(Version xml_version)
        {
            super(xml_version);
        }

        @Override
        public boolean configureFromXML(final ModelReader model_reader, final Widget widget, final Element xml)
                throws Exception
        {
            if (! super.configureFromXML(model_reader, widget, xml))
                return false;

            final BoolButtonWidget button = (BoolButtonWidget) widget;
            // If legacy widgets was configured to not use labels, clear them
            XMLUtil.getChildBoolean(xml, "show_boolean_label").ifPresent(show ->
            {
                if (!show)
                {
                    button.propOffLabel().setValue("");
                    button.propOnLabel().setValue("");
                }
            });
            return true;
        }
    };

    public enum ConfirmDialog
    {
        NONE("No"),
        BOTH("Both"),
        PUSH("Push"),
        RELEASE("Release");

        private final String label;

        private ConfirmDialog(final String label)
        {
            this.label = label;
        }

        @Override
        public String toString()
        {
            return label;
        }
    }

    private static final WidgetPropertyDescriptor<String> propOffImage =
        newFilenamePropertyDescriptor(WidgetPropertyCategory.DISPLAY, "off_image", Messages.WidgetProperties_OffImage);
    private static final WidgetPropertyDescriptor<String> propOnImage =
        newFilenamePropertyDescriptor(WidgetPropertyCategory.DISPLAY, "on_image", Messages.WidgetProperties_OnImage);
    private static final WidgetPropertyDescriptor<Boolean> propShowLED =
        newBooleanPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "show_led", Messages.WidgetProperties_ShowLED);
    private static final WidgetPropertyDescriptor<String> propConfirmMessage =
        newStringPropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "confirm_message", Messages.WidgetProperties_ConfirmMessage);
    private static final WidgetPropertyDescriptor<String> propPassword =
        newStringPropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "password", Messages.WidgetProperties_Password);
    private static final WidgetPropertyDescriptor<ConfirmDialog> propConfirmDialog =
        new WidgetPropertyDescriptor<ConfirmDialog>(WidgetPropertyCategory.BEHAVIOR, "show_confirm_dialog", Messages.WidgetProperties_ConfirmDialog)
    {
        @Override
        public EnumWidgetProperty<ConfirmDialog> createProperty(final Widget widget,
                                                                final ConfirmDialog default_value)
        {
            return new EnumWidgetProperty<ConfirmDialog>(this, widget, default_value);
        }
    };

    private volatile WidgetProperty<Integer> bit;
    private volatile WidgetProperty<String> off_label;
    private volatile WidgetProperty<WidgetColor> off_color;
    private volatile WidgetProperty<String> off_image;
    private volatile WidgetProperty<String> on_label;
    private volatile WidgetProperty<WidgetColor> on_color;
    private volatile WidgetProperty<String> on_image;
    private volatile WidgetProperty<Boolean> show_LED;
    private volatile WidgetProperty<WidgetFont> font;
    private volatile WidgetProperty<WidgetColor> background;
    private volatile WidgetProperty<WidgetColor> foreground;
    private volatile WidgetProperty<Boolean> labels_from_pv;
    private volatile WidgetProperty<Boolean> enabled;
    private volatile WidgetProperty<ConfirmDialog> confirm_dialog;
    private volatile WidgetProperty<String> confirm_message;
    private volatile WidgetProperty<String> password;

    public BoolButtonWidget()
    {
        super(WIDGET_DESCRIPTOR.getType(), 100, 30);
    }

    @Override
    public WidgetConfigurator getConfigurator(Version persisted_version)
            throws Exception
    {
        return new CustomConfigurator(persisted_version);
    }

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(bit = propBit.createProperty(this, 0));
        properties.add(off_label = propOffLabel.createProperty(this, "Off"));
        properties.add(off_color = propOffColor.createProperty(this, new WidgetColor(60, 100, 60)));
        properties.add(off_image = propOffImage.createProperty(this, ""));
        properties.add(on_label = propOnLabel.createProperty(this, "On"));
        properties.add(on_color = propOnColor.createProperty(this, new WidgetColor(60, 255, 60)));
        properties.add(on_image = propOnImage.createProperty(this, ""));
        properties.add(show_LED = propShowLED.createProperty(this, true));
        properties.add(font = propFont.createProperty(this, NamedWidgetFonts.DEFAULT));
        properties.add(foreground = propForegroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.TEXT)));
        properties.add(background = propBackgroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.BUTTON_BACKGROUND)));
        properties.add(labels_from_pv = propLabelsFromPV.createProperty(this, false));
        properties.add(enabled = propEnabled.createProperty(this, true));
        properties.add(confirm_dialog = propConfirmDialog.createProperty(this, ConfirmDialog.NONE));
        properties.add(confirm_message = propConfirmMessage.createProperty(this, "Are your sure you want to do this?"));
        properties.add(password = propPassword.createProperty(this, ""));
    }

    /** @return 'bit' property */
    public WidgetProperty<Integer> propBit()
    {
        return bit;
    }

    /** @return 'off_label' property */
    public WidgetProperty<String> propOffLabel()
    {
        return off_label;
    }

    /** @return 'off_color' property*/
    public WidgetProperty<WidgetColor> propOffColor()
    {
        return off_color;
    }

    /** @return 'off_image' property */
    public WidgetProperty<String> propOffImage()
    {
        return off_image;
    }

    /** @return 'on_label' property */
    public WidgetProperty<String> propOnLabel()
    {
        return on_label;
    }

    /** @return 'on_color' property */
    public WidgetProperty<WidgetColor> propOnColor()
    {
        return on_color;
    }

    /** @return 'on_image' property */
    public WidgetProperty<String> propOnImage()
    {
        return on_image;
    }

    /** @return 'show_LED' property */
    public WidgetProperty<Boolean> propShowLED()
    {
        return show_LED;
    }

    /** @return 'font' property */
    public WidgetProperty<WidgetFont> propFont()
    {
        return font;
    }

    /** @return 'background_color' property */
    public WidgetProperty<WidgetColor> propBackgroundColor()
    {
        return background;
    }

    /** @return 'foreground_color' property */
    public WidgetProperty<WidgetColor> propForegroundColor()
    {
        return foreground;
    }

    /** @return 'labels_from_pv' property */
    public WidgetProperty<Boolean> propLabelsFromPV()
    {
        return labels_from_pv;
    }

    /** @return 'enabled' property */
    public WidgetProperty<Boolean> propEnabled()
    {
        return enabled;
    }

    /** @return 'confirm_dialog' property */
    public WidgetProperty<ConfirmDialog> propConfirmDialog()
    {
        return confirm_dialog;
    }

    /** @return 'confirm_message' property */
    public WidgetProperty<String> propConfirmMessage()
    {
        return confirm_message;
    }

    /** @return 'password' property */
    public WidgetProperty<String> propPassword()
    {
        return password;
    }
}
