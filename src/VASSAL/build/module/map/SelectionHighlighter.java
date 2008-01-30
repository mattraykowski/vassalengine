/*
 * $Id$
 *
 * Copyright (c) 2006-2007 by Brent Easton, Joel Uckelman
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License (LGPL) as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, copies are available
 * at http://www.opensource.org.
 */
package VASSAL.build.module.map;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import VASSAL.build.AbstractConfigurable;
import VASSAL.build.AutoConfigurable;
import VASSAL.build.Buildable;
import VASSAL.build.GameModule;
import VASSAL.build.module.documentation.HelpFile;
import VASSAL.configure.ColorConfigurer;
import VASSAL.configure.Configurer;
import VASSAL.configure.ConfigurerFactory;
import VASSAL.configure.IconConfigurer;
import VASSAL.configure.PropertyExpression;
import VASSAL.configure.VisibilityCondition;
import VASSAL.counters.GamePiece;
import VASSAL.counters.Highlighter;
import VASSAL.tools.ImageUtils;
import VASSAL.tools.imageop.Op;
import VASSAL.tools.imageop.ScaleOp;
import VASSAL.tools.imageop.SourceOp;

public class SelectionHighlighter extends AbstractConfigurable
                                  implements Highlighter {

  public static final String NAME = "name";
  public static final String MATCH = "match";
  public static final String COLOR = "color";
  public static final String THICKNESS = "thickness";
  public static final String USE_IMAGE = "useImage";
  public static final String IMAGE = "image";
  public static final String X_OFFSET = "xoffset";
  public static final String Y_OFFSET = "yoffset";

  protected PropertyExpression matchProperties = new PropertyExpression();
  protected Color color = Color.RED;
  protected int thickness = 3;
  protected boolean useImage = false;
  protected String imageName = "";
  protected int x = 0;
  protected int y = 0;

  protected VisibilityCondition visibilityCondition;

  @Deprecated protected Image image;
  protected SourceOp srcOp;
  protected ScaleOp scaleOp;

  public void draw(GamePiece p, Graphics g, int x, int y,
                   Component obs, double zoom) {
    final Graphics2D g2d = (Graphics2D) g;
    if (accept(p)) {
      if (useImage && srcOp != null) {
        final int x1 = x - (int) (srcOp.getWidth() * zoom / 2);
        final int y1 = y - (int) (srcOp.getHeight() * zoom / 2);
        if (zoom == 1.0) {
          try {
            g2d.drawImage(srcOp.getImage(null), x1, y1, null);
          }
          catch (CancellationException e) {
            e.printStackTrace();
          }
          catch (InterruptedException e) {
            e.printStackTrace();
          }
          catch (ExecutionException e) {
            e.printStackTrace();
          }
        }
        else {
          if (scaleOp == null || scaleOp.getScale() != zoom) {
            scaleOp = Op.scale(srcOp, zoom);
          }

          try {
            g2d.drawImage(scaleOp.getImage(null), x1, y1, null);
          }
          catch (CancellationException e) {
            e.printStackTrace();
          }
          catch (InterruptedException e) {
            e.printStackTrace();
          }
          catch (ExecutionException e) {
            e.printStackTrace();
          }
        }
      }
      else {
        if (color == null || thickness <= 0) {
          return;
        }

        final Shape s = p.getShape();
        final Stroke str = g2d.getStroke();
        g2d.setStroke(new BasicStroke(Math.max(1,Math.round(zoom*thickness))));
        g2d.setColor(color);
        final AffineTransform t = AffineTransform.getScaleInstance(zoom,zoom);
        t.translate(x/zoom,y/zoom);
        g2d.draw(t.createTransformedShape(s));
        g2d.setStroke(str);
      }
    }
  }

  public Rectangle boundingBox(GamePiece p) {
    Rectangle r = p.getShape().getBounds();

    if (accept(p)) {
      if (useImage) {
        if (srcOp != null) {
          r = r.union(ImageUtils.getBounds(srcOp.getSize()));
        }
      }
      else {
        r.translate(-thickness, -thickness);
        r.setSize(r.width + 2 * thickness, r.height + 2 * thickness);
      }
    }
    return r;
  }

  protected boolean accept(GamePiece p) {
    return matchProperties.isNull() ? true : matchProperties.accept(p);
  }
  
  public static String getConfigureTypeName() {
    return "Highlighter";
  }
  
  public String[] getAttributeDescriptions() {
    return new String[] {
      "Name:  ",
      "Active if Properties Match:  ",
      "Use Image",
      "Border Color:  ",
      "Border Thickness:  ",
      "Image:  ",
      "X Offset:  ",
      "Y Offset:  "
    };
  }

  public Class[] getAttributeTypes() {
    return new Class[] {
      String.class,
      PropertyExpression.class,
      Boolean.class,
      Color.class,
      Integer.class,
      IconConfig.class,
      Integer.class,
      Integer.class
    };
  }

  public static class IconConfig implements ConfigurerFactory {
    public Configurer getConfigurer(AutoConfigurable c, String key, String name) {
      return new IconConfigurer(key, name, ((SelectionHighlighter) c).imageName);
    }
  }

  public String[] getAttributeNames() {
    return new String[] {
      NAME,
      MATCH,
      USE_IMAGE,
      COLOR,
      THICKNESS,
      IMAGE,
      X_OFFSET,
      Y_OFFSET
    };
  }

  public VisibilityCondition getAttributeVisibility(String name) {
    if (COLOR.equals(name) || THICKNESS.equals(name)) {
      return new VisibilityCondition() {
        public boolean shouldBeVisible() {
          return !useImage;
        }
      };
    }
    else if (IMAGE.equals(name) ||
             X_OFFSET.equals(name) ||
             Y_OFFSET.equals(name)) {
      return new VisibilityCondition() {
        public boolean shouldBeVisible() {
          return useImage;
        }
      };
    }
    else {
      return super.getAttributeVisibility(name);
    }
  }

  public void setAttribute(String key, Object value) {
    if (key.equals(NAME)) {
      setConfigureName((String) value);
    }
    else if (key.equals(MATCH)) {
      matchProperties.setExpression((String) value);
    }
    else if (key.equals(USE_IMAGE)) {
      if (value instanceof String) {
        value = Boolean.valueOf((String) value);
      }
      useImage = ((Boolean) value).booleanValue();
    }
    else if (key.equals(COLOR)) {
      if (value instanceof String) {
        value = ColorConfigurer.stringToColor((String) value);
      }
      if (value != null) {
        color = ((Color) value);
      }
    }
    else if (key.equals(THICKNESS)) {
      if (value instanceof String) {
        value = new Integer((String) value);
      }
      try {
        thickness = ((Integer) value).intValue();
      }
      catch (NumberFormatException ex) {
      }
    }
    else if (key.equals(IMAGE)) {
      imageName = (String) value;
      srcOp = imageName == null || imageName.trim().length() == 0 
            ? null : Op.load(imageName);
    }
    else if (key.equals(X_OFFSET)) {
      if (value instanceof String) {
        value = new Integer((String) value);
      }
      try {
        x = ((Integer) value).intValue();
      }
      catch (NumberFormatException ex) {
      }
    }
    else if (key.equals(Y_OFFSET)) {
      if (value instanceof String) {
        value = new Integer((String) value);
      }
      try {
        y = ((Integer) value).intValue();
      }
      catch (NumberFormatException ex) {
      }
    }
  }

  public String getAttributeValueString(String key) {
    if (key.equals(NAME)) {
      return getConfigureName();
    }
    else if (key.equals(MATCH)) {
      return matchProperties.getExpression();
    }
    else if (key.equals(USE_IMAGE)) {
      return String.valueOf(useImage);
    }
    else if (key.equals(COLOR)) {
      return ColorConfigurer.colorToString(color);
    }
    else if (key.equals(THICKNESS)) {
      return String.valueOf(thickness);
    }
    else if (key.equals(IMAGE)) {
      return imageName;
    }
    else if (key.equals(X_OFFSET)) {
      return String.valueOf(x);
    }
    else if (key.equals(Y_OFFSET)) {
      return String.valueOf(y);
    }
    return null;
  }

  public void removeFrom(Buildable parent) {
    ((SelectionHighlighters) parent).removeHighlighter(this);
  }

  public HelpFile getHelpFile() {
    return HelpFile.getReferenceManualPage("Map.htm","SelectionHighlighter");
  }

  public Class[] getAllowableConfigureComponents() {
    return new Class[0];
  }

  public void addTo(Buildable parent) {
    ((SelectionHighlighters) parent).addHighlighter(this);
  }
}