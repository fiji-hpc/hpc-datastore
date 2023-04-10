/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2020 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.datastore.zarr;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import cz.it4i.fiji.datastore.core.DatasetDTO;
import cz.it4i.fiji.datastore.core.ViewRegistrationDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


@XmlRootElement
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class DatasetDTOEnchanted {


    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class ResolutionE {

        @Getter
        @Setter
        double value;

        @Getter
        @Setter
        String unit;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @ToString
    public static class ResolutionLevelE {

        public static ResolutionLevelE[] constructLevels(int[][] resolutions,
                                                        int[][] subdivisions)
        {

            ResolutionLevelE[] result = new ResolutionLevelE[resolutions.length];
            for (int i = 0; i < result.length; i++) {
                result[i] = ResolutionLevelE.builder().resolutions(resolutions[i])
                        .blockDimensions(subdivisions[i]).build();
            }
            return result;
        }

        @Getter
        @Setter
        int[] resolutions;

        @Getter
        @Setter
        int[] blockDimensions;

    }

    @Setter
    @Getter
    private String uuid;

    @Setter
    @Getter
    private String voxelType;

    @Setter
    @Getter
    private long[] dimensions;

    @Getter
    @Setter
    @Builder.Default
    private int timepoints = 1;

    @Getter
    @Setter
    @Builder.Default
    private int channels = 1;

    @Getter
    @Setter
    @Builder.Default
    private int angles = 1;

    @Getter
    @Setter
    private double[][] transformations;

    @Getter
    @Setter
    private String voxelUnit;

    @Getter
    @Setter
    private double[] voxelResolution;

    @Getter
    @Setter
    private ResolutionE timepointResolution;

    @Getter
    @Setter
    private ResolutionE channelResolution;

    @Getter
    @Setter
    private ResolutionE angleResolution;



    @Getter
    @Setter
    private String compression;

    @Getter
    @Setter
    private ResolutionLevelE[] resolutionLevels;

    @Getter
    @Setter
    private List<Integer> versions;

    @Getter
    private String label;

    @Getter
    private List<ViewRegistrationDTO> viewRegistrations;

    @Getter
    private List<Integer> timepointIds;

    public void setLabel(String label) {
        this.label = label.replaceAll("[:\\\\/*\"?|<>']", " ");
    }

}