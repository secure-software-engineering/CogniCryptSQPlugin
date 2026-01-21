import React from 'react';

function DetailedDescription ({ description }) {

    return (
        <div>
            {description ? (
                <div dangerouslySetInnerHTML={{ __html: description }}></div>
            ) : (
                <p>Loading...</p>
            )}
        </div>
    );
};

export default DetailedDescription; 