<?php

namespace App\Models;

class ChallengeVideo extends \CodeIgniter\Model
{
    protected $table = 'ChallengeVideo';
    protected $allowedFields = [
        'chal_video_no',
        'chal_no',
        'progress_day',
        'video_create_date',
        'origin_file_name',
        'stored_file_name',
        'file_size'
    ];
    protected $primaryKey = 'chal_video_no';
}
