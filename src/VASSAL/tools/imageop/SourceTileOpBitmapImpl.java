package VASSAL.tools.imageop;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

import VASSAL.tools.HashCode;

/**
 * An {@link ImageOp} for producing tiles directly from a source,
 * without cobbling tiles from the source.
 *
 * @since 3.1.0
 * @author Joel Uckelman
 */
public class SourceTileOpBitmapImpl extends AbstractTileOpImpl {
  private final ImageOp sop;
  private final int x0, y0, x1, y1;
  private final int hash;
 
  public SourceTileOpBitmapImpl(ImageOp sop, int tileX, int tileY) {
    if (sop == null) throw new IllegalArgumentException();

    if (tileX < 0 || tileX >= sop.getNumXTiles() ||
        tileY < 0 || tileY >= sop.getNumYTiles())
      throw new IndexOutOfBoundsException(); 
  
    this.sop = sop;

    final int tw = sop.getTileWidth();
    final int th = sop.getTileHeight();
    final int sw = sop.getWidth();
    final int sh = sop.getHeight();

    x0 = tileX*tw;
    y0 = tileY*th;
    x1 = Math.min((tileX+1)*tw, sw);
    y1 = Math.min((tileY+1)*th, sh);

    size = new Dimension(x1-x0, y1-y0);

    final int PRIME = 31;
    int result = 1;
    result = PRIME * result + HashCode.hash(sop);
    result = PRIME * result + HashCode.hash(x0);
    result = PRIME * result + HashCode.hash(y0);
    result = PRIME * result + HashCode.hash(x1);
    result = PRIME * result + HashCode.hash(y1);
    hash = result;
  }

  public Image apply() throws Exception {
    final BufferedImage dst =
      new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);

    final Graphics2D g = dst.createGraphics();
    g.drawImage(sop.getImage(null), 0, 0, size.width, size.height,
                                    x0, y0, x1, y1, null);
    g.dispose();

    return dst;
  }

  protected void fixSize() { }

  public ImageOp getSource() {
    return null;
  }
 
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || !(o instanceof SourceTileOpBitmapImpl)) return false;

    final SourceTileOpBitmapImpl op = (SourceTileOpBitmapImpl) o;
    return x0 == op.x0 &&
           y0 == op.y0 &&
           x1 == op.x1 &&
           y1 == op.y1 && 
           sop.equals(op.sop);
  }

  @Override
  public int hashCode() {
    return hash;
  }
}