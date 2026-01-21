import React, { useState } from 'react';
import { BaseEdge, EdgeLabelRenderer, getBezierPath } from 'reactflow';
import './CustomAnimatedEdge.css';

const PLUGINKEY = "secai"

export default function CustomAnimatedEdge({
    id, sourceX, sourceY, targetX, targetY,
    style, markerEnd, data
}) {
    const [edgePath] = getBezierPath({ sourceX, sourceY, targetX, targetY });

    return (
        <>
            <BaseEdge
                id={id}
                path={edgePath}
                markerEnd={markerEnd}
                style={style}
                className="custom-edge"
            />
            {data?.isHighlighted && (
                <image
                    href={`/static/${PLUGINKEY}/bug.svg`}
                    width={24}
                    height={24}
                    style={{ pointerEvents: 'none' }}
                //id='animated-image'
                >
                    <animateMotion
                        dur="6s"
                        repeatCount="indefinite"
                        rotate="0"
                        path={edgePath}
                    />
                </image>
            )}
        </>
    );
}
