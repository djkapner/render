/**
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.janelia.alignment.spec.stack;

import java.util.List;

import mpicbg.models.AffineModel2D;

import org.janelia.alignment.spec.Bounds;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the {@link HierarchicalStack} class.
 *
 * @author Eric Trautman
 */
public class HierarchicalStackTest {

    @Test
    public void testEmptyConstructor() throws Exception {
        final HierarchicalStack stack = new HierarchicalStack();
        Assert.assertNotNull("null stack", stack);
    }

    @Test
    public void testGetters() throws Exception {

        final int tier = 2;
        final Integer tierRow = 1;
        final Integer tierColumn = 2;
        final Integer totalTierRowCount = 3;
        final Integer totalTierColumnCount = 4;
        final Double scale = 0.1;
        final Bounds fullScaleBounds = new Bounds(22.0, 33.0, 1.0, 44.0, 55.0, 9.0);

        final StackId roughTilesStackId = new StackId("testOwner", "tilesProject", "roughTiles");
        final StackId parentTierStackId = new StackId("testOwner", "tilesProject", "roughTiles_tier_1_warp_v" + HierarchicalStack.getFullScaleRelativeModelDerivationVersion());
        final StackId warpTilesStackId = new StackId("testOwner", "tilesProject", "roughTiles_tier_2_warp_v" + HierarchicalStack.getFullScaleRelativeModelDerivationVersion());
        final StackId splitStackId = new StackId("testOwner", "tilesProject_roughTiles_tier_2", "0003x0004_000005");

        final HierarchicalStack tier2Stack =
                new HierarchicalStack(roughTilesStackId,
                                      tier,
                                      tierRow,
                                      tierColumn,
                                      totalTierRowCount,
                                      totalTierColumnCount,
                                      scale,
                                      fullScaleBounds);

        checkStackId("invalid rough stack", roughTilesStackId, tier2Stack.getRoughTilesStackId());
        checkStackId("invalid parent stack", parentTierStackId, tier2Stack.getParentTierStackId());
        checkStackId("invalid warp stack", warpTilesStackId, tier2Stack.getWarpTilesStackId());

        Assert.assertEquals("invalid tier", new Integer(tier), tier2Stack.getTier());
        Assert.assertEquals("invalid row", tierRow.intValue(), tier2Stack.getTierRow());
        Assert.assertEquals("invalid column", tierColumn.intValue(), tier2Stack.getTierColumn());
        Assert.assertEquals("invalid row count", totalTierRowCount.intValue(), tier2Stack.getTotalTierRowCount());
        Assert.assertEquals("invalid column count",
                            totalTierColumnCount.intValue(), tier2Stack.getTotalTierColumnCount());
        Assert.assertEquals("invalid scale", scale, tier2Stack.getScale(), DOUBLE_DELTA);

        checkBounds("invalid bounds", fullScaleBounds, tier2Stack.getFullScaleBounds());

        final StackId actualSplitStackId = tier2Stack.getSplitStackId();
        Assert.assertEquals("invalid split stack project", splitStackId.getProject(), actualSplitStackId.getProject());
        Assert.assertEquals("invalid split stack", splitStackId.getStack(), actualSplitStackId.getStack());

        final Long savedMatchPairCount = 22L;
        tier2Stack.setSavedMatchPairCount(savedMatchPairCount);
        Assert.assertEquals("invalid match pair count", savedMatchPairCount, tier2Stack.getSavedMatchPairCount());

        final Double quality = 66.0;
        tier2Stack.setAlignmentQuality(quality);
        Assert.assertEquals("invalid quality", quality, tier2Stack.getAlignmentQuality(), DOUBLE_DELTA);

        Assert.assertNotNull("invalid box path", tier2Stack.getBoxPathForZ(1.0));
    }

    @Test
    public void testGetFullScaleRelativeModel() throws Exception {

        // http://renderer-dev:8080/render-ws/v1/owner/flyTEM/project/trautmane_fafb_fold_rough_tiles_06_AGGREGATED_CONSENSUS_SETS_tier_1/stack/0003x0003_000002/hierarchicalData
        final double splitStackScale = 0.27445725006700616;
        final Bounds fullScaleBounds = new Bounds(44263.0, 38528.0, 2213.0, 47994.0, 42182.0, 2215.0);

        // http://renderer-dev:8080/render-ws/v1/owner/flyTEM/project/trautmane_fafb_fold_rough_tiles_06_AGGREGATED_CONSENSUS_SETS_tier_1/stack/0003x0003_000002_align/z/2213/resolvedTiles
        final AffineModel2D align2213 = getModel(0.993273273471, 0.014838623570, -0.011956136428, 0.999245350615, -514.723796316441, -461.294244130511);

        // http://renderer-dev:8080/render-ws/v1/owner/flyTEM/project/trautmane_fafb_fold_rough_tiles_06_AGGREGATED_CONSENSUS_SETS_tier_1/stack/0003x0003_000002_align/z/2214/resolvedTiles
        final AffineModel2D align2214 = getModel(1.003462201208, -0.023144573264, 0.020596110524, 0.987326006271, -495.611207120813, -579.114722395200);

        final AffineModel2D relative2213 = HierarchicalStack.getFullScaleRelativeModel(align2213,
                                                                                       splitStackScale,
                                                                                       fullScaleBounds);

        final AffineModel2D relative2214 = HierarchicalStack.getFullScaleRelativeModel(align2214,
                                                                                       splitStackScale,
                                                                                       fullScaleBounds);

        final double[] expectedArray = new double[6];
        align2213.toArray(expectedArray);
        expectedArray[4] = 748.4668198145446;
        expectedArray[5] = -481.4776690921208;

        final double[] array = new double[6];
        relative2213.toArray(array);

        for (int i = 0; i < expectedArray.length; i++) {
            Assert.assertEquals("invalid value for index " + i, expectedArray[i], array[i], 0.0001);
        }

        // http://renderer.int.janelia.org:8080/render-ws/v1/owner/flyTEM/project/trautmane_fafb_fold/stack/rough_tiles_06_AGGREGATED_CONSENSUS_SETS/z/2213/box/46140,39900,150,120,1.0/jpeg-image
        final double[] rough2213 = { 46140.0, 39900.0 };

        // http://renderer.int.janelia.org:8080/render-ws/v1/owner/flyTEM/project/trautmane_fafb_fold/stack/rough_tiles_06_AGGREGATED_CONSENSUS_SETS/z/2214/box/45990,40425,150,120,1.0/jpeg-image
        final double[] rough2214 = { 45990.0, 40425.0 };

        final double[] relativelyTransformed2213 = relative2213.apply(rough2213);
        final double[] relativelyTransformed2214 = relative2214.apply(rough2214);

        Assert.assertEquals("invalid transformed X", relativelyTransformed2213[0], relativelyTransformed2214[0], 10.0);
        Assert.assertEquals("invalid transformed Y", relativelyTransformed2213[1], relativelyTransformed2214[1], 10.0);
    }

    @Test
    public void testSplitTier() throws Exception {

        final StackId roughTilesStackId = new StackId("testOwner", "tilesProject", "roughTiles");
        final StackId warpTilesStackId = new StackId("testOwner", "tilesProject", "roughTiles_tier_1_warp_v" + HierarchicalStack.getFullScaleRelativeModelDerivationVersion());
        final Bounds parentStackBounds = new Bounds(54954.0, 58314.0, 1.0, 69539.0, 76856.0, 3.0);
        final int maxPixesPerDimension = 4096;
        final Integer tier = 1;

        final List<HierarchicalStack> splitStacks = HierarchicalStack.splitTier(roughTilesStackId,
                                                                                parentStackBounds,
                                                                                maxPixesPerDimension,
                                                                                tier);

        Assert.assertEquals("invalid number of stacks", 9, splitStacks.size());

        final HierarchicalStack splitStack1 = splitStacks.get(1);

        checkStackId("invalid rough stack", roughTilesStackId, splitStack1.getRoughTilesStackId());
        checkStackId("invalid parent stack", roughTilesStackId, splitStack1.getParentTierStackId());
        checkStackId("invalid warp stack", warpTilesStackId, splitStack1.getWarpTilesStackId());

        Assert.assertEquals("invalid tier", tier, splitStack1.getTier());
        Assert.assertEquals("invalid row", 0, splitStack1.getTierRow());
        Assert.assertEquals("invalid column", 1, splitStack1.getTierColumn());
        Assert.assertEquals("invalid row count", 3, splitStack1.getTotalTierRowCount());
        Assert.assertEquals("invalid column count", 3, splitStack1.getTotalTierColumnCount());
        Assert.assertEquals("invalid scale", 0.662711681588, splitStack1.getScale(), DOUBLE_DELTA);

        // cellWidth = 4862, cellHeight = 6181
        final Bounds expectedSplitStack1Bounds = new Bounds(59816.0, 58314.0, 64678.0, 64495.0);
        checkBounds("invalid bounds", expectedSplitStack1Bounds, splitStack1.getFullScaleBounds());
    }

    private void checkStackId(final String message,
                              final StackId expected,
                              final StackId actual) {
        if (expected.compareTo(actual) != 0) {
            Assert.fail(message + ", expected " + expected + " but was " + actual);
        }
    }

    private void checkBounds(final String message,
                             final Bounds expected,
                             final Bounds actual) {
        Assert.assertEquals(message + " minX", expected.getMinX(), actual.getMinX(), DOUBLE_DELTA);
        Assert.assertEquals(message + " minY", expected.getMinY(), actual.getMinY(), DOUBLE_DELTA);
        Assert.assertEquals(message + " maxX", expected.getMaxX(), actual.getMaxX(), DOUBLE_DELTA);
        Assert.assertEquals(message + " maxY", expected.getMaxY(), actual.getMaxY(), DOUBLE_DELTA);
    }

    private AffineModel2D getModel(final double m00,
                                   final double m10,
                                   final double m01,
                                   final double m11,
                                   final double m02,
                                   final double m12) {
        final AffineModel2D model = new AffineModel2D();
        model.set(m00, m10, m01, m11, m02, m12);
        return model;
    }

    private final double DOUBLE_DELTA = 0.0001;
}
